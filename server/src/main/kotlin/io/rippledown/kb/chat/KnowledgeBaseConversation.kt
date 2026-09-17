package io.rippledown.kb.chat

import io.rippledown.constants.chat.AI_UNAVAILABLE_MESSAGE
import io.rippledown.constants.chat.KB_ACTION_DURING_RULE_MESSAGE
import io.rippledown.constants.chat.NAME_THE_NEW_KB
import io.rippledown.kb.chat.action.*
import io.rippledown.log.lazyLogger
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import kotlinx.coroutines.CancellationException

/**
 * Owns the server's knowledge-base dialogue: greetings, shared naming for new KBs and demonstration copies,
 * and confirmations required by management actions. Explicit [State] keeps the pending question together
 * with the action it authorises, so the server controls transitions and execution after interpreting a reply.
 * [ChatManager] delegates these replies here and calls [reset] for a new conversation, preventing stale consent.
 */
class KnowledgeBaseConversation(
    private val service: KnowledgeBaseService,
    private val interpreter: KbCreationReplyInterpreter,
    private val ruleService: RuleService?
) {
    sealed interface State {
        data object Idle : State
        data object Greeting : State
        data class Creating(
            val stage: KbCreationStage,
            val question: String,
            val actionForName: (String) -> KbManagementAction = { CreateKnowledgeBase(it) },
            val nameQuestion: String = question
        ) : State

        data class Confirming(val offer: KbManagementOutcome.Ask, val resumeNaming: Creating? = null) : State
    }

    var state: State = State.Idle
        private set
    private val logger = lazyLogger

    fun reset() {
        state = State.Idle
    }

    fun greet(currentCase: ViewableCase?, greeting: String): ChatResponse {
        state = if (currentCase == null && service.knowledgeBases().isEmpty() && service.openKnowledgeBase() == null) {
            State.Creating(KbCreationStage.OFFER_CREATION, greeting, nameQuestion = NAME_THE_NEW_KB)
        } else State.Greeting
        return ChatResponse(greeting)
    }

    suspend fun answer(message: String): ChatResponse? = when (val pending = state) {
        State.Idle -> null
        State.Greeting -> {
            reset()
            if (isAcceptance(message) && service.openKnowledgeBase() != null) execute(AddDemonstrationCase()) else null
        }

        is State.Confirming -> when {
            isAcceptance(message) -> {
                reset()
                pending.offer.thenDo(service)
            }

            pending.resumeNaming != null -> answerToNameConfirmation(pending, pending.resumeNaming, message)
            else -> {
                reset()
                null
            }
        }

        is State.Creating -> answerToCreation(pending, message)
    }

    suspend fun execute(action: KbManagementAction): ChatResponse {
        val naming = state as? State.Creating
        reset()
        if (action.changesContext && ruleService?.isRuleSessionActive() == true) {
            return ChatResponse(KB_ACTION_DURING_RULE_MESSAGE)
        }
        return when (val outcome = action.doIt(service)) {
            is KbManagementOutcome.Done -> outcome.response
            is KbManagementOutcome.AskForName -> {
                state = State.Creating(
                    KbCreationStage.AWAITING_NAME, outcome.question, outcome.actionForName, outcome.nameQuestion
                )
                ChatResponse(outcome.question)
            }

            is KbManagementOutcome.Ask -> {
                val resume = naming?.copy(stage = KbCreationStage.AWAITING_NAME, question = naming.nameQuestion)
                state = State.Confirming(outcome, resume)
                ChatResponse(outcome.question)
            }
        }
    }

    private suspend fun answerToCreation(pending: State.Creating, message: String): ChatResponse? {
        if (isAcceptance(message)) return confirmCreation(pending)
        return interpretThen(pending, pending.question, message) { applyReply(pending, it) }
    }

    // A near-duplicate question asked while naming: a refusal or a fresh name
    // continues the naming workflow instead of dropping it.
    private suspend fun answerToNameConfirmation(
        pending: State.Confirming,
        naming: State.Creating,
        message: String
    ): ChatResponse? = interpretThen(naming, pending.offer.question, message) { reply ->
        when (reply.intent) {
            KbCreationIntent.CONFIRM -> {
                reset()
                pending.offer.thenDo(service)
            }

            KbCreationIntent.DENY -> {
                state = naming
                ChatResponse(naming.question)
            }

            KbCreationIntent.UNCLEAR -> ChatResponse(pending.offer.question)
            KbCreationIntent.CONFIRM_WITH_NAME, KbCreationIntent.OTHER_REQUEST -> {
                state = naming
                applyReply(naming, reply)
            }
        }
    }

    private suspend fun interpretThen(
        pending: State.Creating,
        question: String,
        message: String,
        apply: suspend (KbCreationReply) -> ChatResponse?
    ): ChatResponse? {
        val reply = try {
            interpreter.interpret(pending.stage, question, message)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid KB creation interpretation", e)
            return clarify(pending)
        } catch (e: Exception) {
            logger.error("Failed to interpret KB creation reply", e)
            return ChatResponse(AI_UNAVAILABLE_MESSAGE)
        }
        return apply(reply)
    }

    private suspend fun applyReply(pending: State.Creating, reply: KbCreationReply): ChatResponse? =
        when (reply.intent) {
            KbCreationIntent.CONFIRM -> confirmCreation(pending)
            KbCreationIntent.DENY -> {
                reset()
                ChatResponse(KB_CREATION_DECLINED)
            }

            KbCreationIntent.CONFIRM_WITH_NAME -> {
                val name = checkNotNull(reply.kbName).trim()
                val action =
                    if (pending.stage == KbCreationStage.OFFER_CREATION && service.isDemonstrationTitle(name)) {
                        OpenKnowledgeBase(name)
                    } else pending.actionForName(name)
                execute(action)
            }

            KbCreationIntent.UNCLEAR -> clarify(pending)
            KbCreationIntent.OTHER_REQUEST -> {
                reset()
                null
            }
        }

    private fun confirmCreation(pending: State.Creating): ChatResponse {
        val next = if (pending.stage == KbCreationStage.OFFER_CREATION) {
            pending.copy(stage = KbCreationStage.AWAITING_NAME, question = pending.nameQuestion)
        } else pending
        state = next
        return ChatResponse(next.question)
    }

    private fun clarify(pending: State.Creating): ChatResponse {
        val question = when (pending.stage) {
            KbCreationStage.OFFER_CREATION -> KB_CREATION_CLARIFICATION
            KbCreationStage.AWAITING_NAME -> KB_NAME_CLARIFICATION
        }
        state = pending.copy(question = question)
        return ChatResponse(question)
    }

    companion object {
        const val KB_CREATION_DECLINED = "OK, I won't create a knowledge base. You can ask to create one later."
        const val KB_CREATION_CLARIFICATION =
            "A knowledge base holds cases and rules used to generate reports. Would you like to create one? " +
                    "You can also give its name, or say no."
        const val KB_NAME_CLARIFICATION = "What would you like to call the new knowledge base? You can also cancel."
    }
}

private val ACCEPTANCES = setOf(
    "yes", "y", "yes please", "yep", "yeah", "ok", "okay", "sure",
    "correct", "that's right", "do it", "please do",
)

fun isAcceptance(message: String) = message.trim().trimEnd('.', '!').lowercase() in ACCEPTANCES
