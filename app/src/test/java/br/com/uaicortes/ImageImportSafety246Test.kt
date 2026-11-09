package br.com.uaicortes

import java.io.FileNotFoundException
import org.junit.Assert.*
import org.junit.Test

class ImageImportSafety246Test {
    @Test fun mimeShouldTrustDecodedHeader() {
        assertEquals("image/jpeg", ImageImportSafety.recognizedMime("application/octet-stream", "image/jpeg"))
        assertEquals("image/png", ImageImportSafety.recognizedMime(null, "image/png"))
        assertEquals("image/bmp", ImageImportSafety.recognizedMime("image/x-ms-bmp", null))
        assertNull(ImageImportSafety.recognizedMime("image/png", "image/gif"))
        assertNull(ImageImportSafety.recognizedMime("image/gif", null))
    }
    @Test fun errorsShouldNotLeakProviderPaths() {
        assertEquals("A imagem não está mais acessível. Localize novamente o arquivo original.",
            ImageImportSafety.userMessage(FileNotFoundException("content://secret/path")))
        assertFalse(ImageImportSafety.userMessage(IllegalStateException("https://provider/secret"))
            .contains("secret"))
        assertTrue(ImageImportSafety.userMessage(SecurityException("permission://private")).contains("permissão"))
    }
}
