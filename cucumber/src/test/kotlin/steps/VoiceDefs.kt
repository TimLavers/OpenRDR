package steps

import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.kotest.matchers.shouldBe
import org.awaitility.Awaitility.await
import steps.StepsInfrastructure.client
import java.time.Duration.ofSeconds

class VoiceDefs {

    @When("I activate the microphone")
    fun activateMicrophone() {
        chatPO().clickMic()
        await().atMost(ofSeconds(5)).until {
            client().voiceRecognition().isListening.value
        }
    }

    @And("I deactivate the microphone")
    fun deactivateMicrophone() {
        chatPO().clickMic()
        await().atMost(ofSeconds(5)).until {
            !client().voiceRecognition().isListening.value
        }
    }

    @And("I dictate {string}")
    fun dictate(utterance: String) {
        client().voiceRecognition().simulateUtterance(utterance)
    }

    @Then("the chat text field should contain {string}")
    fun chatTextFieldShouldContain(text: String) {
        await().atMost(ofSeconds(10)).until {
            chatPO().chatTextFieldContains(text)
        }
    }

    @Then("the microphone should be active")
    fun microphoneShouldBeActive() {
        client().voiceRecognition().isListening.value shouldBe true
    }

    @Then("the microphone should be inactive")
    fun microphoneShouldBeInactive() {
        client().voiceRecognition().isListening.value shouldBe false
    }
}
