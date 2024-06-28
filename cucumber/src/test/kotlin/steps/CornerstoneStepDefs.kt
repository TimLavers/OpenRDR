package steps

import io.cucumber.java8.En
import io.rippledown.integration.pause

class CornerstoneStepDefs : En {
    init {
        Then("the message {string} should be shown") { message: String ->
        }

        Then("the case {word} is (still )shown as the cornerstone case") { ccName: String ->
            cornerstonePO().requireCornerstoneCase(ccName)
        }
        Then("the case {word} is no longer shown as the cornerstone case") { ccName: String ->
            cornerstonePO().requireCornerstoneCaseNotToBeShowing(ccName)
        }

        Then("the cornerstone case indicator (should )show(s) {int} of {int}") { index: Int, numberOfCornerstoneCases: Int ->
            cornerstonePO().requireIndexAndNumberOfCornerstones(index, numberOfCornerstoneCases)
        }
        When("I click the {word} cornerstone case button") { direction: String ->
            when (direction) {
                "previous" -> cornerstonePO().selectPreviousCornerstoneCase()
                "next" -> cornerstonePO().selectNextCornerstoneCase()
                else -> throw IllegalArgumentException("Unknown direction: $direction")
            }
            pause(2_000) //TODO remove this
        }

        Then("the message indicating no cornerstone cases to review should be shown") {
            cornerstonePO().requireMessageForNoCornerstones()
        }

        When("I approve the cornerstone case") {
            cornerstonePO().exemptCornerstoneCase()
        }
    }
}