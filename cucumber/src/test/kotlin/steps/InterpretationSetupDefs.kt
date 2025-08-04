package steps

import io.cucumber.java.en.And

class InterpretationSetupDefs {
    @And("the interpretation of the case {word} consists of the following comments:")
    fun theInterpretationOfTheCaseConsistsOf(caseName: String, comments: List<String>) {
        comments.forEach { comment ->
            restClient().createRuleToAddText(caseName, comment)
        }
    }
}