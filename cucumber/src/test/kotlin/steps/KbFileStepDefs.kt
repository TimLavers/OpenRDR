package steps

import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.kotest.matchers.shouldBe
import io.rippledown.chat.ChatTestHook
import io.rippledown.files.FileSelection
import io.rippledown.integration.files.ScriptedKbFileDialogs
import io.rippledown.integration.proxy.ConfiguredTestData
import io.rippledown.integration.waitUntilAsserted
import java.io.File
import java.util.zip.ZipFile
import kotlin.io.path.createTempDirectory

class KbFileStepDefs {
    private var exportedZip: File? = null

    @Before("@file-dialogs-are-fake")
    fun configureFileDialogs() {
        StepsInfrastructure.fileDialogs = ScriptedKbFileDialogs()
    }

    private fun dialogs() = checkNotNull(StepsInfrastructure.fileDialogs) {
        "This scenario must be tagged @file-dialogs-are-fake."
    }

    @Given("the file chooser will select the configured KB archive {word}")
    fun selectConfiguredArchive(name: String) {
        dialogs().queueImport(FileSelection.Selected(ConfiguredTestData.kbZipFile(name)))
    }

    @Given("the file chooser will select an export destination")
    fun selectExportDestination() {
        val directory = createTempDirectory("kb-chat-export").toFile().apply { deleteOnExit() }
        val destination = File(directory, "Exported.zip").apply { deleteOnExit() }
        exportedZip = destination
        dialogs().queueExport(FileSelection.Selected(destination))
    }

    @Given("the file chooser will select the previously exported KB archive")
    fun selectPreviouslyExportedArchive() {
        dialogs().queueImport(FileSelection.Selected(checkNotNull(exportedZip)))
    }

    @Given("the import file chooser will be cancelled")
    fun cancelImport() {
        dialogs().queueImport(FileSelection.Cancelled)
    }

    @Given("the export file chooser will be cancelled")
    fun cancelExport() {
        dialogs().queueExport(FileSelection.Cancelled)
    }

    @Then("the exported archive contains a knowledge base")
    fun requireExportedArchive() {
        val archive = checkNotNull(exportedZip)
        waitUntilAsserted(90) {
            ZipFile(archive).use { zip ->
                zip.entries().asSequence().any { it.name.endsWith("Details.txt") } shouldBe true
            }
        }
    }

    @Then("the file chooser has not been opened")
    fun requireNoFileChooser() {
        dialogs().requestCount shouldBe 0
    }

    @Then("the chat history contains {string}")
    fun requireCompletion(text: String) {
        waitUntilAsserted(90) {
            ChatTestHook.snapshot().messageList.any { !it.isUser && it.text.contains(text) } shouldBe true
        }
        waitUntilAsserted(90) { ChatTestHook.snapshot().sendIsEnabled shouldBe true }
    }
}
