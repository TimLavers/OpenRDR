package io.rippledown.integration.files

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.files.FileSelection
import io.rippledown.files.KbFileDialogs
import io.rippledown.files.KbFileTransferController
import io.rippledown.main.Api
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.KbFileDialogRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class KbFileTransferControllerTest {
    private val dialogs = mockk<KbFileDialogs>()
    private val api = mockk<Api>()
    private val messages = mutableListOf<String>()
    private val opened = mutableListOf<KBInfo>()
    private val controller = KbFileTransferController(dialogs, api, { opened.add(it) }, { messages.add(it) })
    private val kb = KBInfo("captured_id", "Clinic")
    private val file = File("Clinic.zip")

    @Test
    fun `import opens the returned KB and reports completion only after transfer`() = runTest {
        // Given
        val transfer = CompletableDeferred<KBInfo>()
        coEvery { dialogs.chooseImportArchive() } returns FileSelection.Selected(file)
        coEvery { api.importKBFromZip(file) } coAnswers { transfer.await() }

        // When
        val job = launch { controller.handle(KbFileDialogRequest.Import("one")) }
        runCurrent()

        // Then
        controller.state shouldBe KbFileTransferController.State.Transferring
        messages shouldBe emptyList()
        opened shouldBe emptyList()
        controller.handle(KbFileDialogRequest.Import("one"))
        transfer.complete(kb)
        job.join()
        opened shouldBe listOf(kb)
        messages shouldBe listOf("Imported \"Clinic\" and opened it.")
        controller.state shouldBe KbFileTransferController.State.Idle
        coVerify(exactly = 1) { api.importKBFromZip(file) }
    }

    @Test
    fun `export uses the request KB and reports the destination after writing`() = runTest {
        // Given
        val transfer = CompletableDeferred<Unit>()
        coEvery { dialogs.chooseExportDestination(kb) } returns FileSelection.Selected(file)
        coEvery { api.exportKBToZip(file, kb) } coAnswers { transfer.await() }

        // When
        val job = launch { controller.handle(KbFileDialogRequest.Export("one", kb)) }
        runCurrent()

        // Then
        messages shouldBe emptyList()
        controller.state shouldBe KbFileTransferController.State.Transferring
        transfer.complete(Unit)
        job.join()
        messages shouldBe listOf("Exported \"Clinic\" to ${file.absolutePath}.")
        opened shouldBe emptyList()
        controller.state shouldBe KbFileTransferController.State.Idle
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `cancelling either chooser performs no transfer and permits another request`(importing: Boolean) = runTest {
        // Given
        coEvery { dialogs.chooseImportArchive() } returns FileSelection.Cancelled
        coEvery { dialogs.chooseExportDestination(kb) } returns FileSelection.Cancelled
        val request = request(importing, "one")

        // When
        controller.handle(request)
        controller.handle(request)
        controller.handle(request(importing, "two"))

        // Then
        val message = if (importing) "Import cancelled." else "Export cancelled."
        messages shouldBe listOf(message, message)
        coVerify(exactly = 0) { api.importKBFromZip(any()) }
        coVerify(exactly = 0) { api.exportKBToZip(any(), any()) }
        controller.state shouldBe KbFileTransferController.State.Idle
    }

    @Test
    fun `duplicate delivery while choosing and after completion opens only one dialog`() = runTest {
        // Given
        val selection = CompletableDeferred<FileSelection>()
        val request = KbFileDialogRequest.Import("one")
        coEvery { dialogs.chooseImportArchive() } coAnswers { selection.await() }
        coEvery { api.importKBFromZip(file) } returns kb

        // When
        val job = launch { controller.handle(request) }
        runCurrent()
        controller.state shouldBe KbFileTransferController.State.ChoosingFile
        controller.handle(request)
        selection.complete(FileSelection.Selected(file))
        job.join()
        controller.handle(request)

        // Then
        coVerify(exactly = 1) { dialogs.chooseImportArchive() }
        coVerify(exactly = 1) { api.importKBFromZip(file) }
        messages.size shouldBe 1
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `transfer failure is reported and releases busy state for retry`(importing: Boolean) = runTest {
        // Given
        coEvery { dialogs.chooseImportArchive() } returns FileSelection.Selected(file)
        coEvery { dialogs.chooseExportDestination(kb) } returns FileSelection.Selected(file)
        coEvery { api.importKBFromZip(file) } throws IOException("Cannot read archive")
        coEvery { api.exportKBToZip(file, kb) } throws IOException("Cannot write destination")

        // When
        controller.handle(request(importing, "one"))
        controller.handle(request(importing, "two"))

        // Then
        val expected =
            if (importing) "Import failed: Cannot read archive" else "Export failed: Cannot write destination"
        messages shouldBe listOf(expected, expected)
        opened shouldBe emptyList()
        controller.state shouldBe KbFileTransferController.State.Idle
    }

    @Test
    fun `native dialog failure is reported without invoking the API`() = runTest {
        // Given
        coEvery { dialogs.chooseImportArchive() } throws IOException("Dialog unavailable")

        // When
        controller.handle(KbFileDialogRequest.Import("one"))

        // Then
        messages shouldBe listOf("Import failed: Dialog unavailable")
        coVerify(exactly = 0) { api.importKBFromZip(any()) }
        controller.state shouldBe KbFileTransferController.State.Idle
    }

    @Test
    fun `coroutine cancellation propagates and releases busy state`() = runTest {
        // Given
        coEvery { dialogs.chooseImportArchive() } throws CancellationException("Closed")

        // When
        shouldThrow<CancellationException> { controller.handle(KbFileDialogRequest.Import("one")) }

        // Then
        messages shouldBe emptyList()
        controller.state shouldBe KbFileTransferController.State.Idle
    }

    @Test
    fun `cancelling a pending transfer releases busy state without claiming success`() = runTest {
        // Given
        val transfer = CompletableDeferred<KBInfo>()
        coEvery { dialogs.chooseImportArchive() } returns FileSelection.Selected(file)
        coEvery { api.importKBFromZip(file) } coAnswers { transfer.await() }

        // When
        val job = launch { controller.handle(KbFileDialogRequest.Import("one")) }
        runCurrent()
        job.cancel()
        job.join()

        // Then
        messages shouldBe emptyList()
        opened shouldBe emptyList()
        controller.state shouldBe KbFileTransferController.State.Idle
    }

    @Test
    fun `another request cannot open a competing dialog while one is pending`() = runTest {
        // Given
        val selection = CompletableDeferred<FileSelection>()
        coEvery { dialogs.chooseImportArchive() } coAnswers { selection.await() }
        coEvery { dialogs.chooseExportDestination(kb) } returns FileSelection.Cancelled
        val export = KbFileDialogRequest.Export("two", kb)

        // When
        val job = launch { controller.handle(KbFileDialogRequest.Import("one")) }
        runCurrent()
        controller.handle(export)

        // Then
        coVerify(exactly = 0) { dialogs.chooseExportDestination(any()) }
        selection.complete(FileSelection.Cancelled)
        job.join()
        controller.handle(export)
        messages shouldBe listOf("Import cancelled.", "Export cancelled.")
    }

    private fun request(importing: Boolean, id: String): KbFileDialogRequest =
        if (importing) KbFileDialogRequest.Import(id) else KbFileDialogRequest.Export(id, kb)
}
