package steps

import io.cucumber.java.en.Then
import io.rippledown.integration.proxy.ConfiguredTestData.trialDataFile

class TrialMatchingStepDefs {
    @Then("I send a case to {string} for each row in the trial-conditions file")
    fun sendTrialConditionsCases(kbName: String) {
        val dataFile = trialDataFile()
        val lines = dataFile.readLines()
        lines.forEach { line ->
            val data = line.split("\t")
            val caseName = data[0]
            val raw = data[1]
            val normalised = data[2]
            val canonical = data[3]
            val attributeNameToValue = mapOf(
                "raw" to raw,
                "normalised" to normalised,
                "canonical" to canonical
            )
            labProxy().provideCaseForKb(kbName, caseName, attributeNameToValue)
        }
    }
}