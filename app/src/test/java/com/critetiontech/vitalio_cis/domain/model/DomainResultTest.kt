package com.critetiontech.vitalio_cis.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DomainResult sealed class.
 * Validates Success/Error branches, data extraction, and exception handling.
 */
class DomainResultTest {

    @Test
    fun `DomainResult Success holds correct data`() {
        val result: DomainResult<String> = DomainResult.Success("Hello")
        assertTrue(result is DomainResult.Success)
        assertEquals("Hello", (result as DomainResult.Success).data)
    }

    @Test
    fun `DomainResult Success with Boolean true`() {
        val result: DomainResult<Boolean> = DomainResult.Success(true)
        assertTrue((result as DomainResult.Success).data)
    }

    @Test
    fun `DomainResult Success with Boolean false`() {
        val result: DomainResult<Boolean> = DomainResult.Success(false)
        assertFalse((result as DomainResult.Success).data)
    }

    @Test
    fun `DomainResult Success with Int list`() {
        val result: DomainResult<List<Int>> = DomainResult.Success(listOf(1, 2, 3))
        val data = (result as DomainResult.Success).data
        assertEquals(3, data.size)
        assertEquals(1, data[0])
    }

    @Test
    fun `DomainResult Success with empty list`() {
        val result: DomainResult<List<String>> = DomainResult.Success(emptyList())
        val data = (result as DomainResult.Success).data
        assertTrue(data.isEmpty())
    }

    @Test
    fun `DomainResult Error holds exception`() {
        val exception = Exception("Network error")
        val result: DomainResult<Nothing> = DomainResult.Error(exception)
        assertTrue(result is DomainResult.Error)
        assertEquals("Network error", (result as DomainResult.Error).exception.message)
    }

    @Test
    fun `DomainResult Error with custom exception type`() {
        val exception = IllegalArgumentException("Bad argument")
        val result = DomainResult.Error(exception)
        assertTrue((result as DomainResult.Error).exception is IllegalArgumentException)
    }

    @Test
    fun `DomainResult Error with null message`() {
        val exception = Exception()
        val result = DomainResult.Error(exception)
        assertNull((result as DomainResult.Error).exception.message)
    }

    @Test
    fun `DomainResult Success is not Error`() {
        val result: DomainResult<Boolean> = DomainResult.Success(true)
        assertFalse(result is DomainResult.Error)
    }

    @Test
    fun `DomainResult Error is not Success`() {
        val result: DomainResult<Nothing> = DomainResult.Error(Exception("err"))
        assertFalse(result is DomainResult.Success)
    }

    @Test
    fun `DomainResult can be matched with when expression - Success branch`() {
        val output = when (val result: DomainResult<Int> = DomainResult.Success(42)) {
            is DomainResult.Success -> "success:${result.data}"
            is DomainResult.Error -> "error:${result.exception.message}"
        }
        assertEquals("success:42", output)
    }

    @Test
    fun `DomainResult can be matched with when expression - Error branch`() {
        val result: DomainResult<Int> = DomainResult.Error(Exception("timeout"))
        val output = when (result) {
            is DomainResult.Success -> "success:${result.data}"
            is DomainResult.Error -> "error:${result.exception.message}"
        }
        assertEquals("error:timeout", output)
    }

    @Test
    fun `DomainResult Success equality holds for same primitive data`() {
        val r1: DomainResult<String> = DomainResult.Success("test")
        val r2: DomainResult<String> = DomainResult.Success("test")
        assertEquals(r1, r2)
    }

    @Test
    fun `DomainResult Success with Unit`() {
        val result: DomainResult<Unit> = DomainResult.Success(Unit)
        assertTrue(result is DomainResult.Success)
    }
}
