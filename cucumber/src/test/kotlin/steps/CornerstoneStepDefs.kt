package steps

import io.cucumber.java.en.Then
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
}