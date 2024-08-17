package steps

import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.rippledown.integration.pause

class CornerstoneStepDefs {
    @Then("the message {string} should be shown")
    fun theMessageStringShouldBeShown(message: String) {
    }

    @Then("the case {word} is (still )shown as the cornerstone case")
    fun theCaseWordIsStillShownAsTheCornerstoneCase(ccName: String) {
        cornerstonePO().requireCornerstoneCase(ccName)
    }

    @Then("the case {word} is no longer shown as the cornerstone case")
    fun theCaseWordIsNoLongerShownAsTheCornerstoneCase(ccName: String) {
        cornerstonePO().requireCornerstoneCaseNotToBeShowing(ccName)
    }

    @Then("the cornerstone case indicator (should )show(s) {int} of {int}")
    fun theCornerstoneCaseIndicatorShouldShowsIntOfInt(index: Int, numberOfCornerstoneCases: Int) {
        cornerstonePO().requireIndexAndNumberOfCornerstones(index, numberOfCornerstoneCases)
    }

    @When("I click the {word} cornerstone case button")
    fun IClickTheWordCornerstoneCaseButton(direction: String) {
        when (direction) {
            "previous" -> cornerstonePO().selectPreviousCornerstoneCase()
            "next" -> cornerstonePO().selectNextCornerstoneCase()
            else -> throw IllegalArgumentException("Unknown direction: $direction")
        }
        pause(2_000) //TODO remove this
    }

    @Then("the message indicating no cornerstone cases to review should be shown")
    fun theMessageIndicatingNoCornerstoneCasesToReviewShouldBeShown() {
        cornerstonePO().requireMessageForNoCornerstones()
    }

    @When("I approve the cornerstone case")
    fun IApproveTheCornerstoneCase() {
        cornerstonePO().exemptCornerstoneCase()
    }
}