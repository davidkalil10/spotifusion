package app.myspoti.host.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginDiagnosticsTest {
    @Test fun `only error identifiers are logged`() {
        assertEquals("login response 400 /login/code/send format=json codes=INVALID_CSRF categories=csrf",
            LoginDiagnostics.summary("""{"path":"/login/code/send","status":400,"format":"json","codes":["INVALID_CSRF","person@example.com","123456","secret-token"],"categories":["csrf","secret-session"],"email":"person@example.com"}"""))
    }

    @Test fun `successful or unrelated responses cannot enter the report`() {
        assertNull(LoginDiagnostics.summary("""{"path":"/login/code/verify","status":200}"""))
        assertNull(LoginDiagnostics.summary("""{"path":"/api/token","status":400}"""))
        assertNull(LoginDiagnostics.summary("x".repeat(2049)))
    }
}
