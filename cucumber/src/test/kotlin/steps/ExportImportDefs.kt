package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.kotest.matchers.shouldBe
import io.rippledown.integration.utils.runMapThroughInterpreterBuiltFromZip
import steps.StepsInfrastructure.uiTestBase
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

    @And("An in-process interpreter using the exported kb gets the interpretation {string} for the input map")
    fun createInProcessInterpreterFromExportedKbAndCheckInterpretation(interpretation: String, map: DataTable) {
        val inputMap = map.asMap(String::class.java, String::class.java)
        val received = runMapThroughInterpreterBuiltFromZip(inputMap,exportedZip!!, uiTestBase.serverProxy.jarFile)
        received shouldBe interpretation
    }
}