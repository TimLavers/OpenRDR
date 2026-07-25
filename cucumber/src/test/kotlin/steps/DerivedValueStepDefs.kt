package steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.kotest.matchers.shouldBe

class DerivedValueStepDefs(val chatDefs: ChatDefs) {

    private var lastDerivedValueName: String? = null

    @When("I request that the derived attribute {string} be added with value {string} for reason {string}")
    fun requestDerivedValueAssignment(attributeName: String, value: String, reason: String) {
        with(chatDefs) {
            enterChatTextAndSend("Assign value \"$value\" to the derived attribute \"$attributeName\"")
            waitForBotSuggestions()
            enterChatTextAndSend(reason)
            decline() //no more reasons
            waitForBotToSayDone()
        }
    }

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

    @Then("the UI should show the value for derived attribute {string} as {string}")
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
    fun uiShouldShowDerivedValueConditions(attributeName: String, conditions: DataTable) {
        interpretationViewPO().waitForDerivedValueConditions(attributeName, conditions.asList())
    }

    @Then("the derived attributes panel should show that there are none for the case")
    fun derivedAttributesPanelShouldShowEmptyState() {
        interpretationViewPO().waitForDerivedValuesEmptyState()
    }

    private fun currentCaseName(): String = caseViewPO().nameShown() ?: error("No case name is currently shown")
}
