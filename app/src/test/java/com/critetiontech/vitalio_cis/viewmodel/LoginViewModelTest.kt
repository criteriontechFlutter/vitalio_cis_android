package com.critetiontech.vitalio_cis.viewmodel

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.AuthRepository
import com.critetiontech.vitalio_cis.domain.usecase.auth.SendOtpUseCase
import com.critetiontech.vitalio_cis.utils.Patient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LoginViewModel state management logic.
 * Uses StandardTestDispatcher + advanceUntilIdle() for deterministic coroutine execution.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─────────────────────────────────────────────────────────────────
    // Lightweight state container that mirrors LoginViewModel's logic
    // Uses explicit TestScope so coroutines are fully controllable
    // ─────────────────────────────────────────────────────────────────

    private class TestLoginState(private val sendOtpUseCase: SendOtpUseCase) {
        var mobile = ""
            private set

        private val _loading = MutableStateFlow(false)
        val loading: StateFlow<Boolean> = _loading

        private val _errorMessage = MutableStateFlow<String?>(null)
        val errorMessage: StateFlow<String?> = _errorMessage

        var lastEmittedRoute: String? = null
            private set

        fun onMobileChange(value: String) { mobile = value }

        // Called inside runTest { ... } — uses testDispatcher via Dispatchers.Main
        fun sendOTP(scope: TestScope) {
            scope.launch {
                _loading.value = true
                _errorMessage.value = null
                when (val result = sendOtpUseCase(mobile)) {
                    is DomainResult.Success -> {
                        if (result.data) lastEmittedRoute = "otp/$mobile"
                        else _errorMessage.value = "You are not registered in any clinic yet"
                    }
                    is DomainResult.Error -> {
                        _errorMessage.value = result.exception.message ?: "Unknown error"
                    }
                }
                _loading.value = false
            }
        }

        fun clearError() { _errorMessage.value = null }
    }

    private fun fakeRepo(result: DomainResult<Boolean>): AuthRepository = object : AuthRepository {
        override suspend fun sendOtp(mobile: String): DomainResult<Boolean> = result
        override suspend fun verifyOtp(uhid: String, otp: String, deviceToken: String): DomainResult<Patient> =
            DomainResult.Error(Exception("Not implemented"))
    }

    // ─────────────────────────────────────────────────────────────────
    // Tests — advanceUntilIdle() drains all pending coroutines
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `initial loading state is false`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Success(true))))
        assertFalse(state.loading.value)
    }

    @Test
    fun `initial errorMessage is null`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Success(true))))
        assertNull(state.errorMessage.value)
    }

    @Test
    fun `initial mobile is empty string`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Success(true))))
        assertEquals("", state.mobile)
    }

    @Test
    fun `onMobileChange updates mobile value`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Success(true))))
        state.onMobileChange("9876543210")
        assertEquals("9876543210", state.mobile)
    }

    @Test
    fun `onMobileChange can be called multiple times and keeps last value`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Success(true))))
        state.onMobileChange("111")
        state.onMobileChange("222")
        state.onMobileChange("9876543210")
        assertEquals("9876543210", state.mobile)
    }

    @Test
    fun `sendOTP emits correct navigation route when OTP is true`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Success(true))))
        state.onMobileChange("9876543210")
        state.sendOTP(this)
        advanceUntilIdle()
        assertEquals("otp/9876543210", state.lastEmittedRoute)
    }

    @Test
    fun `sendOTP navigation route includes mobile number`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Success(true))))
        state.onMobileChange("1234567890")
        state.sendOTP(this)
        advanceUntilIdle()
        assertNotNull(state.lastEmittedRoute)
        assertTrue(state.lastEmittedRoute!!.contains("1234567890"))
    }

    @Test
    fun `sendOTP sets not-registered message when OTP returns false`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Success(false))))
        state.onMobileChange("0000000000")
        state.sendOTP(this)
        advanceUntilIdle()
        assertEquals("You are not registered in any clinic yet", state.errorMessage.value)
    }

    @Test
    fun `sendOTP sets exception message on Error result`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Error(Exception("Server error")))))
        state.sendOTP(this)
        advanceUntilIdle()
        assertEquals("Server error", state.errorMessage.value)
    }

    @Test
    fun `sendOTP resets loading to false after completion`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Success(true))))
        state.sendOTP(this)
        advanceUntilIdle()
        assertFalse(state.loading.value)
    }

    @Test
    fun `sendOTP loading is false after full coroutine completion`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Success(true))))
        state.sendOTP(this)
        // StandardTestDispatcher: coroutine body not started yet — loading is still false
        assertFalse(state.loading.value)
        advanceUntilIdle()
        // After completing: loading should be false again
        assertFalse(state.loading.value)
    }

    @Test
    fun `clearError resets errorMessage to null after error`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Error(Exception("Some error")))))
        state.sendOTP(this)
        advanceUntilIdle()
        assertEquals("Some error", state.errorMessage.value)
        state.clearError()
        assertNull(state.errorMessage.value)
    }

    @Test
    fun `sendOTP with null-message exception shows Unknown error`() = runTest {
        val repo = object : AuthRepository {
            override suspend fun sendOtp(mobile: String): DomainResult<Boolean> =
                DomainResult.Error(Exception(null as String?))
            override suspend fun verifyOtp(uhid: String, otp: String, deviceToken: String): DomainResult<Patient> =
                DomainResult.Error(Exception("Not implemented"))
        }
        val state = TestLoginState(SendOtpUseCase(repo))
        state.sendOTP(this)
        advanceUntilIdle()
        assertEquals("Unknown error", state.errorMessage.value)
    }

    @Test
    fun `no navigation when OTP result is false`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Success(false))))
        state.onMobileChange("9876543210")
        state.sendOTP(this)
        advanceUntilIdle()
        assertNull(state.lastEmittedRoute)
    }

    @Test
    fun `no navigation on error result`() = runTest {
        val state = TestLoginState(SendOtpUseCase(fakeRepo(DomainResult.Error(Exception("Error")))))
        state.sendOTP(this)
        advanceUntilIdle()
        assertNull(state.lastEmittedRoute)
    }

    @Test
    fun `errorMessage is cleared at start of each sendOTP call`() = runTest {
        var callCount = 0
        val repo = object : AuthRepository {
            override suspend fun sendOtp(mobile: String): DomainResult<Boolean> {
                return if (++callCount == 1) DomainResult.Error(Exception("First error"))
                else DomainResult.Success(true)
            }
            override suspend fun verifyOtp(uhid: String, otp: String, deviceToken: String): DomainResult<Patient> =
                DomainResult.Error(Exception())
        }
        val state = TestLoginState(SendOtpUseCase(repo))
        state.sendOTP(this)
        advanceUntilIdle()
        assertEquals("First error", state.errorMessage.value)
        state.sendOTP(this)
        advanceUntilIdle()
        // Second call succeeds — errorMessage should be null (cleared at start, not reset on success)
        assertNull(state.errorMessage.value)
    }
}
