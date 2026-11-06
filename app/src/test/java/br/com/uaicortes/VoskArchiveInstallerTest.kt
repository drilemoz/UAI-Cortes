package br.com.uaicortes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class VoskArchiveInstallerTest {
    private val modelName = "vosk-model-small-pt-0.3"
    private fun root(): File = Files.createTempDirectory("vosk-installer-test").toFile()
    private fun zip(root: File, paths: List<String>): File {
        val archive = File(root, "test.zip")
        ZipOutputStream(archive.outputStream()).use { output ->
            paths.forEach { path ->
                output.putNextEntry(ZipEntry(path))
                output.write(byteArrayOf(1, 2, 3, 4))
                output.closeEntry()
            }
        }
        return archive
    }
    private fun legacy(prefix: String): List<String> = listOf("final.mdl", "mfcc.conf", "HCLr.fst", "Gr.fst")
        .map { "$prefix$it" }
    private fun extract(base: File, archive: File, limit: Long = 1024 * 1024): VoskArchiveInstaller.Result =
        VoskArchiveInstaller.extract(archive, File(base, "unpacked"), modelName, limit)
    private fun expect(problem: VoskArchiveInstaller.Problem, block: () -> Unit) {
        try { block(); fail("Esperava falha $problem") }
        catch (e: VoskArchiveInstaller.InstallException) { assertEquals(problem, e.problem) }
    }

    @Test fun officialPortugueseZipIsAccepted() {
        val base = root()
        try {
            val result = extract(base, zip(base, legacy("$modelName/")))
            assertEquals(4, result.files)
            assertEquals(VoskModelLayout.Format.LEGACY, result.format)
        } finally { base.deleteRecursively() }
    }

    @Test fun zipWithExtraDirectoryIsAccepted() {
        val base = root()
        try {
            val result = extract(base, zip(base, legacy("downloads/$modelName/")))
            assertEquals(VoskModelLayout.Format.LEGACY, result.format)
        } finally { base.deleteRecursively() }
    }

    @Test fun unwrappedLegacyZipIsAccepted() {
        val base = root()
        try { assertEquals(4, extract(base, zip(base, legacy(""))).files) }
        finally { base.deleteRecursively() }
    }

    @Test fun modernModelIsAccepted() {
        val base = root()
        try {
            val modern = listOf("am/final.mdl", "conf/model.conf", "conf/mfcc.conf", "graph/HCLG.fst")
                .map { "$modelName/$it" }
            assertEquals(VoskModelLayout.Format.MODERN, extract(base, zip(base, modern)).format)
        } finally { base.deleteRecursively() }
    }

    @Test fun rejectsOtherModelAndMissingFiles() {
        val base = root()
        try {
            expect(VoskArchiveInstaller.Problem.WRONG_MODEL) {
                extract(base, zip(base, legacy("vosk-model-small-en-us-0.15/")))
            }
            expect(VoskArchiveInstaller.Problem.WRONG_MODEL) {
                extract(base, zip(base, listOf("$modelName/final.mdl")))
            }
        } finally { base.deleteRecursively() }
    }

    @Test fun rejectsUnsafeAndOversizedZip() {
        val base = root()
        try {
            expect(VoskArchiveInstaller.Problem.UNSAFE_PATH) {
                extract(base, zip(base, listOf("$modelName/../escape.txt")))
            }
            assertFalse(File(base, "escape.txt").exists())
            expect(VoskArchiveInstaller.Problem.TOO_LARGE) {
                extract(base, zip(base, legacy("$modelName/")), limit = 12)
            }
        } finally { base.deleteRecursively() }
    }

    @Test fun rejectsInvalidZip() {
        val base = root()
        try {
            val bad = File(base, "bad.zip").apply { writeText("Isso não é um ZIP real") }
            expect(VoskArchiveInstaller.Problem.INVALID_ZIP) { extract(base, bad) }
        } finally { base.deleteRecursively() }
    }

    @Test fun preservesAlreadyInstalledModel() {
        val base = root()
        try {
            val archive = zip(base, legacy("$modelName/"))
            val result = extract(base, archive)
            assertTrue(result.bytes > 0)
            val target = File(base, modelName)
            assertTrue(target.mkdirs())
            legacy("").forEach { path -> File(target, path).writeText("original") }
            assertFalse(VoskArchiveInstaller.installValidated(File(base, "unpacked"), target))
            assertEquals("original", File(target, "final.mdl").readText())
        } finally { base.deleteRecursively() }
    }

    @Test fun replacesBrokenDirectoryOnlyAfterValidation() {
        val base = root()
        try {
            extract(base, zip(base, legacy("$modelName/")))
            val target = File(base, modelName)
            assertTrue(target.mkdirs())
            File(target, "partial.txt").writeText("broken")
            assertTrue(VoskArchiveInstaller.installValidated(File(base, "unpacked"), target))
            assertEquals(VoskModelLayout.Format.LEGACY, VoskModelLayout.detect(target))
            assertFalse(File(target, "partial.txt").exists())
        } finally { base.deleteRecursively() }
    }

    @Test fun restoresValidPreviousModelWhenInstallWasInterrupted() {
        val base = root()
        try {
            extract(base, zip(base, legacy("$modelName/")))
            val backup = File(base, "$modelName.install-previous")
            assertTrue(backup.mkdirs())
            legacy("").forEach { File(backup, it).writeText("previous") }
            val target = File(base, modelName)
            assertTrue(target.mkdirs())
            File(target, "partial.txt").writeText("broken")
            assertFalse(VoskArchiveInstaller.installValidated(File(base, "unpacked"), target))
            assertEquals("previous", File(target, "final.mdl").readText())
        } finally { base.deleteRecursively() }
    }

    @Test fun diagnosticUsesOnlyFixedCodesAndMessages() {
        val error = VoskArchiveInstaller.InstallException(VoskArchiveInstaller.Problem.WRONG_MODEL,
            "Uma URI privada nunca deve ir para diagnóstico")
        assertEquals("wrong_model", VoskInstallFeedback.code(error))
        assertFalse(VoskInstallFeedback.message(error).contains("URI"))
    }
}
