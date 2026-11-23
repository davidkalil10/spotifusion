package app.myspoti.host.android

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.URI

/** A normal mobile WebView for sign-in. Android shares its cookies with the player WebView. */
class MobileSpotifyLogin(
    private val context: Context,
    private val onFailure: () -> Unit = {},
    private val onReturnToPlayer: () -> Unit,
) {
    private var view: WebView? = null
    val isOpen: Boolean get() = view != null

    @SuppressLint("SetJavaScriptEnabled")
    fun open(container: ViewGroup) {
        close()
        val login = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // Keep the engine's default user agent AND client hints during authentication.
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            LoginDiagnostics.install(this)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url.toString()
                    if (request.isForMainFrame && isPlayerReturn(url)) {
                        finishLogin()
                        return true
                    }
                    return !isAllowed(url)
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    // Also handles a main-frame navigation not reported to shouldOverrideUrlLoading.
                    if (isPlayerReturn(url)) finishLogin()
                    else Logbook.log("mobile login page ${Logbook.noQuery(url).take(120)}")
                }

                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                    if (request.url.host == "accounts.spotify.com") {
                        Logbook.log("mobile login http ${response.statusCode} ${request.method} ${request.url.path}")
                    }
                }

                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    Logbook.log("mobile login renderer gone: retry sign-in")
                    release(rendererGone = true)
                    android.os.Handler(android.os.Looper.getMainLooper()).post { onFailure() }
                    return true
                }
            }
            isFocusableInTouchMode = true
        }
        view = login
        container.addView(login, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(login) { v, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                androidx.core.view.WindowInsetsCompat.Type.displayCutout() or
                androidx.core.view.WindowInsetsCompat.Type.ime())
            (v.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
                params.setMargins(bars.left, bars.top, bars.right, bars.bottom)
                v.layoutParams = params
            }
            insets
        }
        androidx.core.view.ViewCompat.requestApplyInsets(login)
        login.requestFocus()
        Logbook.log("mobile login opened: default Android WebView identity, shared cookie store")
        login.loadUrl(WebPlayerHost.LOGIN_URL)
    }

    private fun finishLogin() {
        val login = view ?: return
        // Defer teardown until this WebViewClient callback returns.
        login.post {
            if (view !== login) return@post
            CookieManager.getInstance().flush()
            Logbook.log("mobile login returned to player: checking authenticated session")
            close()
            onReturnToPlayer()
        }
    }

    fun close() = release(rendererGone = false)

    private fun release(rendererGone: Boolean) {
        val login = view ?: return
        view = null
        (login.parent as? ViewGroup)?.removeView(login)
        if (!rendererGone) login.stopLoading()
        login.destroy()
    }

    companion object {
        private val HOSTS = listOf("spotify.com", "spotify.net", "scdn.co", "google.com", "facebook.com", "apple.com")

        internal fun isAllowed(url: String): Boolean = runCatching {
            val uri = URI(url)
            uri.scheme == "https" && uri.rawUserInfo == null && (uri.port == -1 || uri.port == 443) &&
                HOSTS.any { uri.host == it || uri.host?.endsWith(".$it") == true }
        }.getOrDefault(false)

        internal fun isPlayerReturn(url: String): Boolean = runCatching {
            val uri = URI(url)
            isAllowed(url) && uri.host == "open.spotify.com" &&
                !uri.path.orEmpty().split('/').contains("login")
        }.getOrDefault(false)
    }
}
