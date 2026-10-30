package com.critetiontech.ctvitalio.ui.components

import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun <T> ShowNoData(
    isLoading: Boolean,
    list: List<T>?,
    content: @Composable (List<T>) -> Unit
) {

    when {
        isLoading && list.isNullOrEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        !isLoading && list.isNullOrEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.auth_common_no_data_found))
            }
        }

        else -> {
            content(list ?: emptyList())
        }
    }
}