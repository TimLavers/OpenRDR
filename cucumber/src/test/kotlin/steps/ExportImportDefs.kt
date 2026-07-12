package steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import java.io.File

class ExportImportDefs {
    private var exportedZip: File? = null
    @And("I export the current Knowledge Base")
    fun exportTheCurrentKnowledgeBase() {
        exportedZip = File.createTempFile("Exported", ".zip")
        kbControlsPO().exportKB(exportedZip!!.absolutePath)
    }

    @Given("I import the previously exported Knowledge Base")
    fun importThePreviouslyExportedKnowledgeBase() {
        require(exportedZip != null) {
            "Import of previously exported KB attempted but exported KB is null."
        }
        kbControlsPO().importKB(exportedZip!!.absolutePath)
    }

    @And("An in-process interpreter using the exported kb gets the interpretation for the case value map")
    fun createInProcessInterpreterFromExportedKbAndCheckInterpretation() {
//        val interpreter = KbLoader().createInterpreter(exportedZip!!.absolutePath)
//        val received = interpreter.interpret(mapOf("" to "value"))
//        received.interpretation.conclusionTexts() shouldBe setOf()
    }

}