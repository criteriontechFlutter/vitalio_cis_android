package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.data.repository.ActivityRepositoryImpl
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ActivityRepository
import com.critetiontech.ctvitalio.model.ActivityModel
import com.critetiontech.ctvitalio.model.ActivitySaveResult
import com.critetiontech.ctvitalio.model.AddPatientActivityRequest
import com.critetiontech.ctvitalio.model.PatientActivityItem
import com.critetiontech.ctvitalio.ui.screens.formatActivityTimeRange
import com.critetiontech.ctvitalio.utils.Patient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    // ── Fakes ──

    private class FakeRepo : ActivityRepository {
        var master: DomainResult<List<ActivityModel>> = DomainResult.Success(emptyList())
        var addResult: DomainResult<ActivitySaveResult> = DomainResult.Success(ActivitySaveResult.SAVED)
        var fetchResult: DomainResult<List<PatientActivityItem>> = DomainResult.Success(emptyList())
        var removeResult: DomainResult<Unit> = DomainResult.Success(Unit)

        val addRequests = mutableListOf<AddPatientActivityRequest>()
        val fetchCalls = mutableListOf<Triple<String, String, Int>>()
        val removeCalls = mutableListOf<Triple<Int, Int, Int>>()

        override suspend fun fetchActivityMaster() = master
        override suspend fun addPatientActivity(request: AddPatientActivityRequest): DomainResult<ActivitySaveResult> {
            addRequests += request; return addResult
        }
        override suspend fun fetchPatientActivity(uhid: String, date: String, clientId: Int): DomainResult<List<PatientActivityItem>> {
            fetchCalls += Triple(uhid, date, clientId); return fetchResult
        }
        override suspend fun removePatientActivity(id: Int, clientId: Int, userId: Int): DomainResult<Unit> {
            removeCalls += Triple(id, clientId, userId); return removeResult
        }
    }

    private fun patient() = Patient(
        pid = 7, uhId = "UHID-TEST", firstName = "Test", lastName = "User",
        dob = "1990-01-01", genderId = 1, countryCallingCode = "+91",
        mobileNo = "9000000000", emailAddress = "t@example.com",
        age = "34", guardianRelationId = 0, guardianName = "",
        countryId = 1, stateId = 1, cityId = 1, address = "",
        imageUrl = "", bloodGroupId = 1, zip = "000000", isActive = true,
        clientId = 99, departmentName = null, cityName = "",
        stateName = "", countryName = "",
        genderName = "", guardianRelationName = null, bloodGroupName = ""
    )

    /** 17 Nov 2026, [hour]:[minute]:42 local time. */
    private fun at(hour: Int, minute: Int): Date = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 17, hour, minute, 42); set(Calendar.MILLISECOND, 123)
    }.time

    private fun vm(repo: FakeRepo, loggedIn: Boolean = true, now: Date = at(10, 0)) =
        ActivityViewModel(repo, currentPatient = { if (loggedIn) patient() else null }, now = { now })

    private fun item(id: Int, activityId: Int = 1, name: String = "Biking") =
        PatientActivityItem(serverId = id, activityId = activityId, activityName = name, timeFrom = "09:00:00", totalTimeInMinutes = "30")

    // ── Opening the sheet / times ──

    @Test fun `openActivity selects it and defaults to the last 30 minutes`() {
        val vm = vm(FakeRepo(), now = at(10, 0))
        vm.openActivity("Biking", 1)
        assertEquals("Biking", vm.selectedActivity)
        assertEquals(1, vm.selectedActivityId)
        assertEquals(30L, vm.durationMinutes)
        assertTrue(vm.isTimeRangeValid)
    }

    @Test fun `openActivity just after midnight never starts on the previous day`() {
        val vm = vm(FakeRepo(), now = at(0, 10))
        vm.openActivity("Biking", 1)
        assertEquals(10L, vm.durationMinutes)
        assertEquals(0, Calendar.getInstance().apply { time = vm.startTime }.get(Calendar.HOUR_OF_DAY))
    }

    @Test fun `picked times drop seconds so duration is exact`() {
        val vm = vm(FakeRepo())
        vm.openActivity("Biking", 1)
        vm.updateStartTime(7, 0)
        vm.updateEndTime(8, 15)
        assertEquals(75L, vm.durationMinutes)
    }

    @Test fun `end before start is invalid instead of being silently clamped`() {
        val vm = vm(FakeRepo())
        vm.openActivity("Biking", 1)
        vm.updateStartTime(9, 0)
        vm.updateEndTime(8, 0)
        assertFalse(vm.isTimeRangeValid)
        assertEquals(9, Calendar.getInstance().apply { time = vm.startTime }.get(Calendar.HOUR_OF_DAY))
        assertEquals(8, Calendar.getInstance().apply { time = vm.endTime }.get(Calendar.HOUR_OF_DAY))
    }

    // ── Saving ──

    @Test fun `save sends the logged-in patient, chosen activity, start time and minutes`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = vm(repo)
        vm.openActivity("Aerobics", 2)
        vm.updateStartTime(7, 5)
        vm.updateEndTime(7, 50)
        vm.insertActivity()
        advanceUntilIdle()

        val req = repo.addRequests.single()
        assertEquals("UHID-TEST", req.uhid)
        assertEquals(99, req.clientID)
        assertEquals(2, req.activityId)
        assertEquals("07:05:00", req.timeFrom)
        assertEquals("45", req.totalTimeInMinutes)
        assertTrue(vm.saveSuccess.value)
        assertEquals(R.string.track_activity_saved, vm.message.value)
        assertNull(vm.selectedActivityId)
        assertEquals(1, repo.fetchCalls.size) // list refreshed after save
        assertFalse(vm.isSaving.value)
    }

    @Test fun `save without a logged-in patient sends nothing`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = vm(repo, loggedIn = false)
        vm.openActivity("Biking", 1)
        vm.insertActivity()
        advanceUntilIdle()
        assertTrue(repo.addRequests.isEmpty())
        assertEquals(R.string.track_activity_login_required, vm.message.value)
    }

    @Test fun `save without a selected activity sends nothing`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = vm(repo)
        vm.insertActivity()
        advanceUntilIdle()
        assertTrue(repo.addRequests.isEmpty())
        assertEquals(R.string.track_activity_select_first, vm.message.value)
    }

    @Test fun `save with zero or negative duration sends nothing`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = vm(repo)
        vm.openActivity("Biking", 1)
        vm.updateStartTime(8, 0)
        vm.updateEndTime(8, 0)
        vm.insertActivity()
        advanceUntilIdle()
        assertTrue(repo.addRequests.isEmpty())
        assertEquals(R.string.track_activity_end_after_start, vm.message.value)
    }

    @Test fun `server already-exists answer is reported, not treated as saved`() = runTest(dispatcher) {
        val repo = FakeRepo().apply { addResult = DomainResult.Success(ActivitySaveResult.ALREADY_EXISTS) }
        val vm = vm(repo)
        vm.openActivity("Biking", 1)
        vm.insertActivity()
        advanceUntilIdle()
        assertFalse(vm.saveSuccess.value)
        assertEquals(R.string.track_activity_already_logged, vm.message.value)
        assertEquals(1, vm.selectedActivityId) // sheet keeps the user's input
    }

    @Test fun `save failure shows an error and keeps the sheet`() = runTest(dispatcher) {
        val repo = FakeRepo().apply { addResult = DomainResult.Error(Exception("500")) }
        val vm = vm(repo)
        vm.openActivity("Biking", 1)
        vm.insertActivity()
        advanceUntilIdle()
        assertFalse(vm.saveSuccess.value)
        assertEquals(R.string.track_activity_save_failed, vm.message.value)
        assertFalse(vm.isSaving.value)
    }

    // ── Activity master ──

    @Test fun `master list comes only from the server`() = runTest(dispatcher) {
        val repo = FakeRepo().apply { master = DomainResult.Success(listOf(ActivityModel(1, "Biking"), ActivityModel(2, "Aerobics"))) }
        val vm = vm(repo)
        vm.getAllActivities()
        advanceUntilIdle()
        assertEquals(listOf(1, 2), vm.allActivities.value.map { it.id })
        assertFalse(vm.masterLoadFailed.value)
    }

    @Test fun `master failure shows retry instead of a fake list with wrong ids`() = runTest(dispatcher) {
        val repo = FakeRepo().apply { master = DomainResult.Error(Exception("offline")) }
        val vm = vm(repo)
        vm.getAllActivities()
        advanceUntilIdle()
        assertTrue(vm.allActivities.value.isEmpty())
        assertTrue(vm.masterLoadFailed.value)
    }

    @Test fun `master failure after a good load keeps the list`() = runTest(dispatcher) {
        val repo = FakeRepo().apply { master = DomainResult.Success(listOf(ActivityModel(1, "Biking"))) }
        val vm = vm(repo)
        vm.getAllActivities(); advanceUntilIdle()
        repo.master = DomainResult.Error(Exception("offline"))
        vm.getAllActivities(); advanceUntilIdle()
        assertEquals(1, vm.allActivities.value.size)
        assertFalse(vm.masterLoadFailed.value)
    }

    // ── Today's list ──

    @Test fun `fetch uses patient uhid, clientId and today's date`() = runTest(dispatcher) {
        val repo = FakeRepo().apply { fetchResult = DomainResult.Success(listOf(item(5))) }
        val vm = vm(repo)
        vm.fetchPatientActivity()
        advanceUntilIdle()
        assertEquals(Triple("UHID-TEST", "2026-11-17", 99), repo.fetchCalls.single())
        assertEquals(listOf(5), vm.recentActivityList.value.map { it.serverId })
    }

    @Test fun `an empty answer clears a stale list`() = runTest(dispatcher) {
        val repo = FakeRepo().apply { fetchResult = DomainResult.Success(listOf(item(5))) }
        val vm = vm(repo)
        vm.fetchPatientActivity(); advanceUntilIdle()
        repo.fetchResult = DomainResult.Success(emptyList())
        vm.fetchPatientActivity(); advanceUntilIdle()
        assertTrue(vm.recentActivityList.value.isEmpty())
    }

    @Test fun `fetch without a patient makes no call`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = vm(repo, loggedIn = false)
        vm.fetchPatientActivity(); advanceUntilIdle()
        assertTrue(repo.fetchCalls.isEmpty())
    }

    // ── History date ──

    @Test fun `history starts on today`() {
        val vm = vm(FakeRepo())
        assertEquals(LocalDate.of(2026, 11, 17), vm.historyDate.value)
        assertTrue(vm.isHistoryToday)
    }

    @Test fun `previous day loads that date`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = vm(repo)
        vm.shiftHistoryDate(-1)
        advanceUntilIdle()
        assertEquals(LocalDate.of(2026, 11, 16), vm.historyDate.value)
        assertFalse(vm.isHistoryToday)
        assertEquals("2026-11-16", repo.fetchCalls.single().second)
    }

    @Test fun `cannot move past today`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = vm(repo)
        vm.shiftHistoryDate(1)
        vm.setHistoryDate(LocalDate.of(2027, 1, 1))
        advanceUntilIdle()
        assertEquals(LocalDate.of(2026, 11, 17), vm.historyDate.value)
        assertTrue(repo.fetchCalls.isEmpty()) // already on today: nothing to reload
    }

    @Test fun `picking a calendar date loads it and clears the old day's rows`() = runTest(dispatcher) {
        val repo = FakeRepo().apply { fetchResult = DomainResult.Success(listOf(item(5))) }
        val vm = vm(repo)
        vm.loadHistory(); advanceUntilIdle()
        repo.fetchResult = DomainResult.Success(emptyList())
        vm.setHistoryDate(LocalDate.of(2026, 10, 2))
        assertTrue(vm.recentActivityList.value.isEmpty()) // cleared immediately, before the answer
        advanceUntilIdle()
        assertEquals("2026-10-02", repo.fetchCalls.last().second)
    }

    @Test fun `a slow answer for an older date never overwrites the newer date`() = runTest(dispatcher) {
        val repo = object : ActivityRepository by FakeRepo() {
            override suspend fun fetchPatientActivity(uhid: String, date: String, clientId: Int): DomainResult<List<PatientActivityItem>> =
                if (date == "2026-11-16") { delay(5_000); DomainResult.Success(listOf(item(16))) }
                else DomainResult.Success(listOf(item(15)))
        }
        val vm = ActivityViewModel(repo, currentPatient = { patient() }, now = { at(10, 0) })
        vm.shiftHistoryDate(-1) // 16th: slow
        vm.shiftHistoryDate(-1) // 15th: fast
        advanceUntilIdle()
        assertEquals(LocalDate.of(2026, 11, 15), vm.historyDate.value)
        assertEquals(listOf(15), vm.recentActivityList.value.map { it.serverId })
    }

    // ── Delete ──

    @Test fun `delete calls the patient-activity endpoint with row id and clientId`() = runTest(dispatcher) {
        val repo = FakeRepo().apply { fetchResult = DomainResult.Success(listOf(item(5), item(6))) }
        val vm = vm(repo)
        vm.fetchPatientActivity(); advanceUntilIdle()
        vm.removeActivity(vm.recentActivityList.value.first())
        advanceUntilIdle()
        assertEquals(Triple(5, 99, 0), repo.removeCalls.single())
        assertEquals(listOf(6), vm.recentActivityList.value.map { it.serverId })
    }

    @Test fun `failed delete puts the row back and tells the user`() = runTest(dispatcher) {
        val repo = FakeRepo().apply {
            fetchResult = DomainResult.Success(listOf(item(5), item(6)))
            removeResult = DomainResult.Error(Exception("500"))
        }
        val vm = vm(repo)
        vm.fetchPatientActivity(); advanceUntilIdle()
        vm.removeActivity(vm.recentActivityList.value.first())
        advanceUntilIdle()
        assertEquals(listOf(5, 6), vm.recentActivityList.value.map { it.serverId })
        assertEquals(R.string.track_activity_delete_failed, vm.message.value)
    }

    @Test fun `row without a server id is never sent for delete`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = vm(repo)
        vm.removeActivity(item(0))
        advanceUntilIdle()
        assertTrue(repo.removeCalls.isEmpty())
    }

    // ── Names (server returns the right activityId but a wrong activityName) ──

    @Test fun `display name comes from the master list by activityId`() = runTest(dispatcher) {
        val repo = FakeRepo().apply { master = DomainResult.Success(listOf(ActivityModel(1, "Biking"), ActivityModel(7, "Biathlon"))) }
        val vm = vm(repo)
        val row = item(17, activityId = 1, name = "Biathlon") // what the live API answered on 17 Nov 2026
        assertEquals("Biathlon", vm.displayName(row)) // master not loaded yet: server name
        vm.getAllActivities(); advanceUntilIdle()
        assertEquals("Biking", vm.displayName(row))
        assertEquals("Run", vm.displayName(item(18, activityId = 99, name = "Run"))) // unknown id
    }

    @Test fun `server's 400 No record found is an empty day, not an error`() {
        assertTrue(ActivityRepositoryImpl.isNoRecord("""{"status":0,"message":"failure","responseValue":"No record found"}"""))
        assertFalse(ActivityRepositoryImpl.isNoRecord("""{"status":0,"message":"failure","responseValue":"Conversion failed when converting date"}"""))
        assertFalse(ActivityRepositoryImpl.isNoRecord("Server error 400"))
    }

    // ── Parsing + display ──

    @Test fun `parses wrapped and bare-array responses`() {
        val wrapped = """{"status":1,"message":"ok","responseValue":[{"id":3,"activityId":1,"activityName":"Biking","timeFrom":"07:00:00","totalTimeInMinutes":"20"}]}"""
        val bare = """[{"id":4,"activityId":2,"activityName":"Aerobics"}]"""
        assertEquals(3, ActivityRepositoryImpl.parsePatientActivities(wrapped).single().serverId)
        assertEquals("Aerobics", ActivityRepositoryImpl.parsePatientActivities(bare).single().activityName)
        assertTrue(ActivityRepositoryImpl.parsePatientActivities("").isEmpty())
        assertTrue(ActivityRepositoryImpl.parsePatientActivities("""{"status":1,"responseValue":[]}""").isEmpty())
    }

    @Test fun `history time range is computed, never invented`() {
        assertEquals("7:30 AM – 8:15 AM · 45 min", formatActivityTimeRange("07:30:00", "45"))
        assertEquals("07:30 – 08:15 · 45 min", formatActivityTimeRange("07:30", "45", is24h = true))
        assertEquals("7:30 AM", formatActivityTimeRange("07:30:00", null))
        assertEquals("45 min", formatActivityTimeRange("", "45"))
        assertEquals("—", formatActivityTimeRange("", null))
        assertEquals("—", formatActivityTimeRange("garbage", "0"))
    }
}
