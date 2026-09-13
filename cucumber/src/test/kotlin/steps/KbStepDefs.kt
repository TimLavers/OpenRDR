package steps

import io.cucumber.java.en.Then
import io.kotest.matchers.shouldBe
import io.rippledown.constants.kb.NO_KB_SELECTED
import io.rippledown.integration.waitUntilAsserted
import io.rippledown.sample.SampleKB

class KbStepDefs {

    @Then("the displayed KB name is (now ){word}")
    fun theDisplayedKBNameIsNow(kbName: String) {
        waitUntilAsserted {
            kbControlsPO().currentKB() shouldBe kbName
        }
    }

    @Then("the displayed KB name is (now ){string}")
    fun theDisplayedKBNameIsNowQuoted(kbName: String) {
        // given / when / then
        theDisplayedKBNameIsNow(kbName)
    }

    @Then("no knowledge base is shown as selected")
    fun noKnowledgeBaseIsShownAsSelected() {
        waitUntilAsserted {
            kbControlsPO().currentKB() shouldBe NO_KB_SELECTED
        }
    }

    @Then("A Knowledge Base called {word} has been created from the {string} sample")
    fun createKnowledgeBaseFromSample(kbName: String, sampleTitle: String) {
        val sample = SampleKB.entries.single { it.title() == sampleTitle }
        restClient().createKBFromSample(kbName, sample)
    }

    @Then("the KB controls (are )(should be )hidden")
    fun theKBControlsAreShouldBeHidden() {
        kbControlsPO().requireKbControlsToBeHidden()
    }

    @Then("the KB controls (are )(should be )shown")
    fun theKBControlsAreShouldBeShown() {
        kbControlsPO().requireKbControlsToBeShown()
    }
}
