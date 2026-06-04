package com.critetiontech.vitalio_cis.domain.model

sealed class DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>()
    data class Error(val exception: Exception) : DomainResult<Nothing>()
}
