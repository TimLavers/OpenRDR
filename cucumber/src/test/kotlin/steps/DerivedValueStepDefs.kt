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

    private fun currentCaseName(): String = caseViewPO().nameShown() ?: error("No case name is currently shown")
}
