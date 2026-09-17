package io.rippledown.integration.files

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.files.FileDialogLauncher
import io.rippledown.files.FileKitKbFileDialogs
import io.rippledown.files.FileSelection
import io.rippledown.model.KBInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.awt.Window
import java.io.File
import java.io.IOException

class KbFileDialogsTest {
    @TempDir
    lateinit var directory: File
    private val window = mockk<Window>()
    private val launcher = FakeFileDialogLauncher()
    private val dialogs = FileKitKbFileDialogs(window, launcher)

    @Test
    fun `import selects a single ZIP from a dialog owned by the application window`() = runTest {
        // Given
        val archive = File(directory, "Clinic.zip").apply { writeText("archive") }
        launcher.selection = archive

        // When
        val result = dialogs.chooseImportArchive()

        // Then
        result shouldBe FileSelection.Selected(archive)
        launcher.parent shouldBe window
        launcher.title shouldBe "Import KB"
        launcher.extensions shouldBe setOf("zip")
        launcher.openCalls shouldBe 1
        launcher.saveCalls shouldBe 0
        archive.readText() shouldBe "archive"
    }

    @Test
    fun `import cancellation is explicit`() = runTest {
        // Given
        launcher.selection = null

        // When
        val result = dialogs.chooseImportArchive()

        // Then
        result shouldBe FileSelection.Cancelled
    }

    @Test
    fun `export suggests the KB name and ZIP extension without creating a file`() = runTest {
        // Given
        val kb = KBInfo("thyroids_1", "Thyroid Function")
        val destination = File(directory, "My backup.ZIP")
        launcher.selection = destination

        // When
        val result = dialogs.chooseExportDestination(kb)

        // Then
        result shouldBe FileSelection.Selected(destination)
        launcher.parent shouldBe window
        launcher.title shouldBe "Export KB"
        launcher.suggestedName shouldBe "Thyroid Function"
        launcher.extensions shouldBe setOf("zip")
        launcher.saveCalls shouldBe 1
        launcher.openCalls shouldBe 0
        destination.exists() shouldBe false
        kb.name shouldBe "Thyroid Function"
    }

    @Test
    fun `save cancellation leaves an existing file untouched`() = runTest {
        // Given
        val destination = File(directory, "Thyroids.zip").apply { writeText("original archive") }
        launcher.selection = null

        // When
        val result = dialogs.chooseExportDestination(KBInfo("kb_1", "Thyroids"))

        // Then
        result shouldBe FileSelection.Cancelled
        destination.readText() shouldBe "original archive"
    }

    @Test
    fun `confirmed existing destination is returned unchanged without overwriting it`() = runTest {
        // Given
        val destination = File(directory, "existing.zip").apply { writeText("original archive") }
        launcher.selection = destination

        // When
        val result = dialogs.chooseExportDestination(KBInfo("kb_1", "Thyroids"))

        // Then
        result shouldBe FileSelection.Selected(destination)
        destination.readText() shouldBe "original archive"
    }

    @ParameterizedTest
    @CsvSource(
        "Thyroids,Thyroids", "Clinic / West,Clinic _ West", "Clinic:North,Clinic_North",
        "CON,_CON", "con.txt,_con.txt", "PRN,_PRN", "AUX,_AUX", "NUL,_NUL",
        "COM1,_COM1", "LPT9,_LPT9", "COM¹,_COM¹", "LPT²,_LPT²", "COM10,COM10", "CONditions,CONditions",
        "...,knowledge-base", "Zoo Animals.,Zoo Animals", "Thyroïde,Thyroïde"
    )
    fun `suggested export names are valid without renaming the KB`(name: String, expected: String) = runTest {
        // Given
        val kb = KBInfo("kb_1", name)

        // When
        dialogs.chooseExportDestination(kb)

        // Then
        launcher.suggestedName shouldBe expected
        kb.name shouldBe name
    }

    @Test
    fun `filename suggestion replaces all Windows forbidden characters and trims trailing dots and spaces`() = runTest {
        // Given
        val kb = KBInfo("kb_1", "Clinic<>:\"/\\|?*\t . ")

        // When
        dialogs.chooseExportDestination(kb)

        // Then
        launcher.suggestedName shouldBe "Clinic__________"
    }

    @Test
    fun `dialog failures propagate rather than looking like cancellation`() = runTest {
        // Given
        val failure = IOException("Native chooser failed")
        launcher.failure = failure

        // When / Then
        shouldThrow<IOException> { dialogs.chooseImportArchive() } shouldBe failure
        shouldThrow<IOException> { dialogs.chooseExportDestination(KBInfo("kb_1", "Thyroids")) } shouldBe failure
    }

    @Test
    fun `coroutine cancellation propagates`() = runTest {
        // Given
        val cancellation = CancellationException("Closing application")
        launcher.failure = cancellation

        // When / Then
        shouldThrow<CancellationException> { dialogs.chooseImportArchive() } shouldBe cancellation
        shouldThrow<CancellationException> {
            dialogs.chooseExportDestination(
                KBInfo(
                    "kb_1",
                    "Thyroids"
                )
            )
        } shouldBe cancellation
    }

    private class FakeFileDialogLauncher : FileDialogLauncher {
        var selection: File? = null
        var failure: Exception? = null
        var parent: Window? = null
        var title: String? = null
        var extensions: Set<String>? = null
        var suggestedName: String? = null
        var openCalls = 0
        var saveCalls = 0

        override suspend fun openFile(parent: Window, title: String, extensions: Set<String>): File? {
            this.parent = parent
            this.title = title
            this.extensions = extensions
            openCalls++
            failure?.let { throw it }
            return selection
        }

        override suspend fun saveFile(parent: Window, title: String, suggestedName: String, extension: String): File? {
            this.parent = parent
            this.title = title
            this.suggestedName = suggestedName
            this.extensions = setOf(extension)
            saveCalls++
            failure?.let { throw it }
            return selection
        }
    }
}
