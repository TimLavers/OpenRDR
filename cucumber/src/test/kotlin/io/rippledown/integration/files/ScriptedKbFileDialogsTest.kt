package io.rippledown.integration.files

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.files.FileSelection
import io.rippledown.model.KBInfo
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.File

class ScriptedKbFileDialogsTest {
    @Test
    fun `queued import selections are consumed once in order`() = runTest {
        // Given
        val dialogs = ScriptedKbFileDialogs()
        val archive = File("archive.zip")
        dialogs.queueImport(FileSelection.Selected(archive))
        dialogs.queueImport(FileSelection.Cancelled)

        // When
        val first = dialogs.chooseImportArchive()
        val second = dialogs.chooseImportArchive()

        // Then
        first shouldBe FileSelection.Selected(archive)
        second shouldBe FileSelection.Cancelled
        dialogs.requestCount shouldBe 2
        shouldThrow<IllegalStateException> { dialogs.chooseImportArchive() }
    }

    @Test
    fun `export and import selections are independent`() = runTest {
        // Given
        val dialogs = ScriptedKbFileDialogs()
        val destination = File("export.zip")
        dialogs.queueImport(FileSelection.Cancelled)
        dialogs.queueExport(FileSelection.Selected(destination))
        dialogs.queueExport(FileSelection.Cancelled)

        // When
        val export = dialogs.chooseExportDestination(KBInfo("clinic", "Clinic"))
        val cancelledExport = dialogs.chooseExportDestination(KBInfo("clinic", "Clinic"))
        val cancelledImport = dialogs.chooseImportArchive()

        // Then
        export shouldBe FileSelection.Selected(destination)
        cancelledExport shouldBe FileSelection.Cancelled
        cancelledImport shouldBe FileSelection.Cancelled
        dialogs.requestCount shouldBe 3
        shouldThrow<IllegalStateException> { dialogs.chooseExportDestination(KBInfo("clinic", "Clinic")) }
    }

    @Test
    fun `a new scenario starts with no requests or queued selections`() = runTest {
        // Given
        val previous = ScriptedKbFileDialogs()
        previous.queueImport(FileSelection.Cancelled)
        previous.chooseImportArchive()

        // When
        val next = ScriptedKbFileDialogs()

        // Then
        next.requestCount shouldBe 0
        shouldThrow<IllegalStateException> { next.chooseImportArchive() }
    }
}
