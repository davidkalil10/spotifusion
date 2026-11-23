package app.myspoti.host.android

import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

/** Observes error identifiers from Spotify's login page; never repeats a login request. */
internal object LoginDiagnostics {
    private const val ORIGIN = "https://accounts.spotify.com"

    fun install(view: WebView) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            Logbook.log("login diagnostics: response observer unavailable")
            return
        }
        WebViewCompat.addWebMessageListener(view, "MyspotiLoginDiagnostics", setOf(ORIGIN)) { _, message, origin, mainFrame, _ ->
            if (mainFrame && origin.toString() == ORIGIN) {
                runCatching { summary(message.data ?: "") }.getOrNull()?.let(Logbook::log)
            }
        }
        val script = view.context.assets.open("login-diagnostics.js").bufferedReader().use { it.readText() }
        WebViewCompat.addDocumentStartJavaScript(view, script, setOf(ORIGIN))
        Logbook.log("login diagnostics: response observer installed")
    }

    internal fun summary(message: String): String? {
        if (message.length > 2048) return null
        val json = JSONObject(message)
        val path = json.optString("path")
        val status = json.optInt("status")
        if (path !in setOf("/login/code/send", "/login/code/verify") || status !in 400..599) return null
        fun values(key: String, valid: (String) -> Boolean): String {
            val array = json.optJSONArray(key) ?: return "none"
            return (0 until minOf(array.length(), 8)).map { array.optString(it) }
                .filter(valid).joinToString(",").ifEmpty { "none" }
        }
        val codes = values("codes") { it.length <= 64 && CODE.matches(it) }
        val categories = values("categories") { it in CATEGORIES }
        val format = json.optString("format").takeIf { it in setOf("json", "non-json", "large") } ?: "unknown"
        return "login response $status $path format=$format codes=$codes categories=$categories"
    }

    private val CODE = Regex("(?:[A-Z]+_)+[A-Z]+|(?:[a-z]+_)+[a-z]+")
    private val CATEGORIES = setOf("captcha", "csrf", "rate-limit", "session", "browser", "email", "invalid-request")
}
