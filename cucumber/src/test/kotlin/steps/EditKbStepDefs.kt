package steps

import io.cucumber.docstring.DocString
import io.cucumber.java.en.And
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.kotest.matchers.shouldBe
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds

class EditKbStepDefs {

    private val chatDefs = ChatDefs()

    @When("I set the KB description to:")
    fun i_set_the_description_to(description: DocString) {
        val descriptionOperator = editCurrentKbControlPO().showDescriptionOperator()
        descriptionOperator.setDescription(description.content)
    }

    @Then("the KB description is:")
    fun the_KBDescriptionIsNow(description: DocString) {
        val expectedText = description.content ?: ""
        val descriptionOperator = editCurrentKbControlPO().showDescriptionOperator()
        descriptionOperator.description() shouldBe expectedText
        descriptionOperator.cancel()
    }

    /**
     * Asks the chatbot to undo the last rule. The bot replies with a description
     * of the rule and a request for confirmation; the next chat turn (an
     * affirmation or a refusal) decides whether the undo actually happens.
     *
     * @see io.rippledown.kb.chat.action.ShowLastRuleForUndo
     */
    @When("I ask the chatbot to undo the last rule")
    fun i_ask_the_chatbot_to_undo_the_last_rule() {
        chatDefs.enterChatTextAndSend("undo the last rule")
    }

    @And("I confirm the undo")
    fun i_confirm_the_undo() {
        // The bot must have produced its preview / "reply yes to confirm" message
        // before we send the affirmation, otherwise the LLM has no context for
        // interpreting "yes" as an undo confirmation.
        chatDefs.waitForBotText("Please confirm")
        val countBefore = chatPO().numberOfChatMessages()
        chatDefs.enterChatTextAndSend("yes")
        await().atMost(ofSeconds(60)).until {
            chatPO().numberOfChatMessages() > countBefore + 1
        }
    }

    @Then("the chatbot says there are no rules to undo")
    fun chatbot_says_there_are_no_rules_to_undo() {
        chatDefs.waitForBotText("no rules to undo")
    }
}