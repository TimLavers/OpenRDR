package steps

import io.cucumber.java8.En

class CornerstoneStepDefs : En {
    init {
        Then("the message {string} should be shown") { message: String ->
        }

        Then("the case {word} is (still )shown as the cornerstone case") { ccName: String ->
//            cornerstonePO.requireCornerstoneCase(ccName)
        }

        Then("the number of cornerstone cases should be shown as {int}") { numberOfCornerstoneCases: Int ->
//            cornerstonePO.requireNumberOfCornerstones(numberOfCornerstoneCases)
        }
        When("I click the {word} cornerstone case button") { direction: String ->
            when (direction) {
                "previous" -> cornerstonePO().selectPreviousCornerstoneCase()
                "next" -> cornerstonePO().selectNextCornerstoneCase()
            }
        }

        Then("the message indicating no cornerstone cases to review should be shown") {
            cornerstonePO().requireMessageForNoCornerstones()
        }
    }
}