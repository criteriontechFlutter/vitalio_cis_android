package com.critetiontech.ctvitalio.ui.components

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.critetiontech.ctvitalio.R

/**
 * Waving care team shown in the pull-to-refresh header. A transparent animated WebP made from
 * the design video (no sound); it animates on Android 9+, older phones show the first frame.
 */
@Composable
fun DoctorsWaveAnimation(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val loader = remember(context) {
        ImageLoader.Builder(context)
            .components { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(ImageDecoderDecoder.Factory()) }
            .build()
    }
    val request = remember(context) {
        ImageRequest.Builder(context).data(R.raw.doctors_wave).crossfade(false).build()
    }
    AsyncImage(
        model = request,
        imageLoader = loader,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
