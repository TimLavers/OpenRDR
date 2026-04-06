package steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.rippledown.constants.cornerstone.CORNERSTONE_TITLE

class CornerstoneStepDefs {

    @Then("the cornerstone index of {int} and total of {int} is displayed")
    fun theCornerstoneIndexAndTotalIsDisplayed(index: Int, total: Int) {
        cornerstonePO().requireCornerstoneLabel("$CORNERSTONE_TITLE $index of $total")
    }

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

    @Then("there are no cornerstone cases showing")
    fun requireNoCornerstoneCases() {
        cornerstonePO().requireNoCornerstoneCases()
    }

    @Then("the message indicating no cornerstone cases to review should be shown")
    fun requireNoCornerstonesToReviewMessage() {
        cornerstonePO().requireNoCornerstonesToReviewMessage()
    }

    @And("the cornerstone case indicator shows {int} of {int}")
    fun theCornerstoneCaseIndicatorShows(index: Int, total: Int) {
        cornerstonePO().requireCornerstoneIndicator(index, total)
    }

    @When("I approve the cornerstone case")
    fun approveTheCornerstoneCase() {
        cornerstonePO().clickExemptButton()
    }
}