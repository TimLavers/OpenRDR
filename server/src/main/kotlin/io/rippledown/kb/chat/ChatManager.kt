package io.rippledown.kb.chat

import io.rippledown.chat.conversation.ConversationService
import io.rippledown.model.RDRCase
import io.rippledown.model.rule.RuleTreeChange

interface RuleService {
    suspend fun buildRule(case: RDRCase, ruleTreeChange: RuleTreeChange): String

}

class ChatManager(val conversationService: ConversationService, ruleService: RuleService) {
    lateinit var currentCase: RDRCase

    suspend fun startConversation(case: RDRCase): String {
        currentCase = case
        return conversationService.startConversation(case)
    }

    suspend fun response(message: String): String {
        val response = conversationService.response(message)
        //if response is a rule change, call ruleService.buildRule
        return response
    }
}
