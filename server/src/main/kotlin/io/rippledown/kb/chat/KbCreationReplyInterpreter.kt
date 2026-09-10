package io.rippledown.kb.chat

import io.rippledown.chat.ConversationService
import io.rippledown.stripEnclosingJson
import io.rippledown.toJsonString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class KbCreationStage { OFFER_CREATION, AWAITING_NAME }

enum class KbCreationIntent { CONFIRM, DENY, CONFIRM_WITH_NAME, UNCLEAR, OTHER_REQUEST }

@Serializable
data class KbCreationReply(val intent: KbCreationIntent, val kbName: String? = null)

/** Interprets a reply to a server-owned question; it cannot execute an action. */
class KbCreationReplyInterpreter(private val conversation: ConversationService) {
    suspend fun interpret(stage: KbCreationStage, question: String, message: String): KbCreationReply {
        val context = mapOf("stage" to stage.name, "question" to question, "userReply" to message)
        val response = conversation.response("$instructions\n${context.toJsonString()}")
        val reply = Json.decodeFromString<KbCreationReply>(response.stripEnclosingJson())
        require(
            if (reply.intent == KbCreationIntent.CONFIRM_WITH_NAME) !reply.kbName.isNullOrBlank()
            else reply.kbName == null
        ) { "The interpretation must supply a name only for CONFIRM_WITH_NAME" }
        return reply
    }

    companion object {
        private val instructions = checkNotNull(
            KbCreationReplyInterpreter::class.java.getResource("/chat/kb_creation_reply.md")
        ).readText()
    }
}
