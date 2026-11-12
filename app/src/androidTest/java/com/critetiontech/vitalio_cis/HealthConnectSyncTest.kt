package com.critetiontech.ctvitalio

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.critetiontech.ctvitalio.healthconnect.VitalMetric
import com.critetiontech.ctvitalio.healthconnect.WearableData
import com.critetiontech.ctvitalio.healthconnect.WearableSync
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * End-to-end check of the wearable sync: seeds Health Connect the way a watch app would,
 * runs the real sync (read + AddVital/AddPatientActivity API calls) and verifies the results.
 * Needs a logged-in session on the device, and posts one vitals entry and one workout to the server.
 */
@RunWith(AndroidJUnit4::class)
class HealthConnectSyncTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var client: HealthConnectClient

    @Before
    fun setUp() {
        assumeTrue(HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE)
        WearableData.RECORD_TYPES
            .flatMap { listOf(HealthPermission.getReadPermission(it), HealthPermission.getWritePermission(it)) }
            .forEach { instrumentation.uiAutomation.grantRuntimePermission(context.packageName, it) }
        client = HealthConnectClient.getOrCreate(context)
    }

    @Test
    fun syncSavesNewWearableDataOnce() = runBlocking {
        val granted = client.permissionController.getGrantedPermissions()
        assertEquals(emptySet<String>(), WearableData.READ_PERMISSIONS - granted)

        // Clear data from earlier runs; an app can only delete records it wrote itself
        WearableData.RECORD_TYPES.forEach {
            client.deleteRecords(it, TimeRangeFilter.after(Instant.now().minus(30, ChronoUnit.DAYS)))
        }

        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val zone = ZoneOffset.systemDefault().rules.getOffset(now)
        val older = now.minus(2, ChronoUnit.HOURS)
        val latest = now.minus(1, ChronoUnit.MINUTES)
        val meta = { Metadata.manualEntry() }
        client.insertRecords(
            listOf(
                // Older reading must not win over the newer one
                HeartRateRecord(older, zone, older.plusSeconds(60), zone, listOf(HeartRateRecord.Sample(older, 60)), meta()),
                HeartRateRecord(latest, zone, latest.plusSeconds(30), zone,
                    listOf(HeartRateRecord.Sample(latest, 70), HeartRateRecord.Sample(latest.plusSeconds(20), 78)), meta()),
                BloodPressureRecord(time = latest, zoneOffset = zone, metadata = meta(),
                    systolic = Pressure.millimetersOfMercury(122.0), diastolic = Pressure.millimetersOfMercury(81.0)),
                OxygenSaturationRecord(time = latest, zoneOffset = zone, percentage = Percentage(97.0), metadata = meta()),
                RespiratoryRateRecord(time = latest, zoneOffset = zone, rate = 16.0, metadata = meta()),
                BodyTemperatureRecord(time = latest, zoneOffset = zone, metadata = meta(), temperature = Temperature.celsius(37.0))
            )
        )

        val first = WearableSync.sync(context, client)
        assertNull("sync failed: ${first.error}", first.error)
        val values = first.snapshot.vitals.associate { it.metric to it.value }
        assertEquals("78", values[VitalMetric.HEART_RATE])
        assertEquals("122/81", values[VitalMetric.BLOOD_PRESSURE])
        assertEquals("97", values[VitalMetric.SPO2])
        assertEquals("16", values[VitalMetric.RESPIRATORY_RATE])
        assertEquals("98.6", values[VitalMetric.BODY_TEMPERATURE])
        assertEquals(5, first.vitalsSaved)
        assertTrue(first.snapshot.vitals.all { WearableSync.isSaved(context, it) })

        // Running again must not send the same readings twice
        val second = WearableSync.sync(context, client)
        assertNull(second.error)
        assertEquals(0, second.vitalsSaved)
    }
}
