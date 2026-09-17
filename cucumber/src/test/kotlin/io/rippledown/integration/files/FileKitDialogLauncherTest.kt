package io.rippledown.integration.files

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import io.rippledown.files.FileKitDialogLauncher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.awt.Window
import java.io.File

class FileKitDialogLauncherTest {
    private val window = mockk<Window>()

    @BeforeEach
    fun stubNativeEntryPoints() {
        mockkStatic("io.github.vinceglb.filekit.dialogs.FileKitKt")
        mockkStatic("io.github.vinceglb.filekit.dialogs.FileKit_nonWebKt")
    }

    @AfterEach
    fun restoreNativeEntryPoints() {
        unmockkAll()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `open delegates to the single file picker with the owner and filter`(cancelled: Boolean) = runTest {
        // Given
        val chosen = if (cancelled) null else File("Clinic.zip")
        val settings = slot<FileKitDialogSettings>()
        val type = slot<FileKitType>()
        coEvery {
            FileKit.openFilePicker(type = capture(type), directory = null, dialogSettings = capture(settings))
        } returns chosen?.let(::PlatformFile)

        // When
        val result = FileKitDialogLauncher.openFile(window, "Import KB", setOf("zip"))

        // Then
        result shouldBe chosen
        settings.captured.parentWindow shouldBe window
        settings.captured.title shouldBe "Import KB"
        type.captured.shouldBeInstanceOf<FileKitType.File>().extensions shouldBe setOf("zip")
        coVerify(exactly = 1) { FileKit.openFilePicker(any(), null, any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `save delegates to the native saver with the owner and ZIP extension`(cancelled: Boolean) = runTest {
        // Given
        val chosen = if (cancelled) null else File("My backup.ZIP")
        val settings = slot<FileKitDialogSettings>()
        coEvery {
            FileKit.openFileSaver(
                suggestedName = "Thyroids", defaultExtension = "zip", allowedExtensions = setOf("zip"),
                directory = null, dialogSettings = capture(settings)
            )
        } returns chosen?.let(::PlatformFile)

        // When
        val result = FileKitDialogLauncher.saveFile(window, "Export KB", "Thyroids", "zip")

        // Then
        result shouldBe chosen
        settings.captured.parentWindow shouldBe window
        settings.captured.title shouldBe "Export KB"
        coVerify(exactly = 1) {
            FileKit.openFileSaver(
                "Thyroids", defaultExtension = "zip", allowedExtensions = setOf("zip"),
                directory = null, dialogSettings = any()
            )
        }
    }
}
