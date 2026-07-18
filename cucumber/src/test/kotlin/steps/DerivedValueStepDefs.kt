package steps

import io.cucumber.java.en.Then
import io.kotest.matchers.shouldBe

class DerivedValueStepDefs {

    @Then("the derived value {string} should be {string}")
    fun derivedValueShouldBe(attributeName: String, expectedValue: String) {
        val actual = restClient().derivedValueFor(currentCaseName(), attributeName)
        actual shouldBe expectedValue
    }

    @Then("the derived value {string} should not be present")
    fun derivedValueShouldNotBePresent(attributeName: String) {
        val actual = restClient().derivedValueFor(currentCaseName(), attributeName)
        actual shouldBe null
    }

    @Then("the UI should show the derived value {string} as {string}")
    fun uiShouldShowDerivedValue(attributeName: String, expectedValue: String) {
        interpretationViewPO().waitForDerivedValueToBeShown(attributeName, expectedValue)
    }

    @Then("the UI should show the formula {string} for the derived value {string}")
    fun uiShouldShowDerivedValueFormula(formula: String, attributeName: String) {
        interpretationViewPO().waitForDerivedValueFormula(attributeName, formula)
    }

    @Then("the UI should show the following conditions for the derived value {string}:")
    fun uiShouldShowDerivedValueConditions(attributeName: String, conditions: io.cucumber.datatable.DataTable) {
        interpretationViewPO().waitForDerivedValueConditions(attributeName, conditions.asList())
    }

    @Then("the derived values panel should be hidden")
    fun derivedValuesPanelShouldBeHidden() {
        interpretationViewPO().requireDerivedValuesPanelToBeHidden()
    }

    private fun currentCaseName(): String = caseViewPO().nameShown() ?: error("No case name is currently shown")
}
