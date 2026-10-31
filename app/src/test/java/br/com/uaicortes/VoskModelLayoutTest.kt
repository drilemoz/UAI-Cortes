package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VoskModelLayoutTest {
    private fun temporaryRoot(): File = Files.createTempDirectory("vosk-layout").toFile()
    private fun File.add(relative: String) {
        val file = File(this, relative)
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(1))
    }

    @Test fun officialPortugueseArchiveUsesLegacyLayout() {
        val root = temporaryRoot()
        try {
            listOf("final.mdl", "mfcc.conf", "HCLr.fst", "Gr.fst").forEach { root.add(it) }
            assertEquals(VoskModelLayout.Format.LEGACY, VoskModelLayout.detect(root))
        } finally { root.deleteRecursively() }
    }

    @Test fun modernLayoutRemainsSupported() {
        val root = temporaryRoot()
        try {
            listOf("am/final.mdl", "conf/model.conf", "conf/mfcc.conf", "graph/HCLG.fst")
                .forEach { root.add(it) }
            assertEquals(VoskModelLayout.Format.MODERN, VoskModelLayout.detect(root))
        } finally { root.deleteRecursively() }
    }

    @Test fun missingOrEmptyFilesMustBeRejected() {
        val root = temporaryRoot()
        try {
            listOf("final.mdl", "mfcc.conf", "HCLr.fst").forEach { root.add(it) }
            assertNull(VoskModelLayout.detect(root))
            File(root, "Gr.fst").createNewFile()
            assertNull(VoskModelLayout.detect(root))
        } finally { root.deleteRecursively() }
    }
}
