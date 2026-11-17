package com.critetiontech.ctvitalio.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.AppColors

/** Which legal page to show; the route argument is the enum name. */
enum class LegalPage(val url: String) {
    PRIVACY("https://criteriontech.in/privacy-policy.html"),
    TERMS("https://criteriontech.in/terms-of-use.html");

    val titleRes: Int
        get() = if (this == PRIVACY) R.string.auth_legal_privacy_title else R.string.auth_legal_terms_title
}

/**
 * The criteriontech.in pages carry the whole website (navbar, breadcrumb banner, footer,
 * cookie bar). Hide everything except the policy text in `section.care-content`.
 */
private const val CONTENT_ONLY_CSS =
    "header,nav,.page-breadcrumb,footer,#cookies,.cookie-wrapper{display:none!important}" +
        "body{padding-top:0!important;margin-top:0!important}" +
        ".care-content{padding-top:12px!important;margin-top:0!important}"

private const val INJECT_CSS_JS =
    "(function(){var id='vitalio-content-only';if(document.getElementById(id))return;" +
        "var s=document.createElement('style');s.id=id;s.innerHTML='$CONTENT_ONLY_CSS';" +
        "(document.head||document.documentElement).appendChild(s);})();"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LegalWebViewScreen(page: LegalPage) {
    var loading by remember { mutableStateOf(true) }

    CommonAppBar(title = stringResource(page.titleRes)) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        // Stay hidden until the site chrome is stripped, so it never flashes.
                        visibility = View.INVISIBLE
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                loading = true
                                view.visibility = View.INVISIBLE
                            }

                            override fun onPageCommitVisible(view: WebView, url: String?) {
                                view.evaluateJavascript(INJECT_CSS_JS, null)
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                view.evaluateJavascript(INJECT_CSS_JS) {
                                    view.visibility = View.VISIBLE
                                    loading = false
                                }
                            }

                            // Links inside the policy (mailto, other sites) open outside the app.
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val uri = request.url
                                if (uri.toString() == page.url) return false
                                runCatching {
                                    view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri.toString())))
                                }
                                return true
                            }
                        }
                        loadUrl(page.url)
                    }
                },
            )
            if (loading) {
                CircularProgressIndicator(color = AppColors.Primary, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
