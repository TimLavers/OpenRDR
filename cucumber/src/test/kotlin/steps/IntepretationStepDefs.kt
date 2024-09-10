package steps

import io.cucumber.java.en.And

class IntepretationStepDefs {
    @And("move pointer to the comment {string}")
    fun movePointerToComment(text: String) {
        interpretationViewPO().movePointerToComment(text)
    }
}