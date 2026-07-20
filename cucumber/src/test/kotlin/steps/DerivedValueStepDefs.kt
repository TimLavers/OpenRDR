package steps

import io.cucumber.java.en.Then
import io.kotest.matchers.shouldBe

class DerivedValueStepDefs {

    private var lastDerivedValueName: String? = null

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
        lastDerivedValueName = attributeName
        interpretationViewPO().waitForDerivedValueToBeShown(attributeName, expectedValue)
    }

    @Then("the formula showing for the derived value is {string}")
    fun formulaShowingForDerivedValue(formula: String) {
        val attributeName = lastDerivedValueName ?: error("No derived value has been checked in this scenario")
        interpretationViewPO().waitForDerivedValueFormula(attributeName, formula)
    }

    @Then("the UI should show the following conditions for the derived value {string}:")
    fun uiShouldShowDerivedValueConditions(attributeName: String, conditions: io.cucumber.datatable.DataTable) {
        interpretationViewPO().waitForDerivedValueConditions(attributeName, conditions.asList())
    }

    @Then("the derived attributes panel should be hidden")
    fun derivedAttributesPanelShouldBeHidden() {
        interpretationViewPO().requireDerivedValuesPanelToBeHidden()
    }

    private fun currentCaseName(): String = caseViewPO().nameShown() ?: error("No case name is currently shown")
}
