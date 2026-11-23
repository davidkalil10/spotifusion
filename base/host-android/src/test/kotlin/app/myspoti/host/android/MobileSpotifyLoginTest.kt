package app.myspoti.host.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileSpotifyLoginTest {
    @Test fun `only secure trusted destinations can open`() {
        assertTrue(MobileSpotifyLogin.isAllowed("https://accounts.spotify.com/pt-BR/login"))
        assertTrue(MobileSpotifyLogin.isAllowed("https://accounts.google.com/"))
        listOf("http://accounts.spotify.com/", "https://evilspotify.com/", "https://spotify.com.evil.example/",
            "https://open.spotify.com@evil.example/", "https://user@open.spotify.com/", "https://open.spotify.com:444/",
            "intent://open.spotify.com/", "invalid url").forEach { assertFalse(it, MobileSpotifyLogin.isAllowed(it)) }
    }

    @Test fun `only main player destinations trigger session verification`() {
        assertTrue(MobileSpotifyLogin.isPlayerReturn("https://open.spotify.com/"))
        assertTrue(MobileSpotifyLogin.isPlayerReturn("https://open.spotify.com/intl-pt/"))
        assertFalse(MobileSpotifyLogin.isPlayerReturn("https://accounts.spotify.com/login"))
        assertFalse(MobileSpotifyLogin.isPlayerReturn("https://open.spotify.com/intl-pt/login"))
        assertFalse(MobileSpotifyLogin.isPlayerReturn("https://open.spotify.com.evil.example/"))
    }
}
