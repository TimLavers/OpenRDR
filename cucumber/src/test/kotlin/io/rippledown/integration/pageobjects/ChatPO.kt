package io.rippledown.integration.pageobjects

import io.rippledown.chat.*
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAll
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleEditableText

// ORD2
class ChatPO(private val contextProvider: () -> AccessibleContext) {

    private fun chatTextContext() =
        execute<AccessibleContext> { contextProvider().find(CHAT_TEXT_FIELD) }

    private fun chatEditableTextContext() =
        execute<AccessibleEditableText> { chatTextContext().accessibleEditableText }

    fun enterChatText(text: String) {
        await().atMost(ofSeconds(10)).until {
            try {
                execute { chatEditableTextContext()?.setTextContents(text) }
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun clickSend() {
        execute { chatTextContext().find(CHAT_SEND)?.accessibleAction?.doAccessibleAction(0) }
    }


    fun mostRecentBotRowContainsTerms(terms: List<String>): Boolean {
        val numberOfChatMessages = numberOfChatMessages()
        return execute<Boolean> {
            val matcher = { context: AccessibleContext ->
                terms.all { term -> context.foundText(term) } && context.isBotResponseForIndex(numberOfChatMessages - 1)
            }
            contextProvider().find(matcher) != null
        }
    }
    fun mostRecentBotRowContainsAnyOfTheTerms(terms: List<String>): Boolean {
        val numberOfChatMessages = numberOfChatMessages()
        return execute<Boolean> {
            val botMatcher = { context: AccessibleContext ->
                context.isBotResponseForIndex(numberOfChatMessages - 1)
            }
            val botRow = contextProvider().find(botMatcher) ?: return@execute false
            terms.any { term -> botRow.find({ ctx -> ctx.foundText(term) }) != null }
        }
    }

    fun mostRecentSuggestionRowContainsTerms(terms: List<String>): Boolean {
        return execute<Boolean> {
            val suggestionNodes = contextProvider().findAll({ ctx ->
                ctx.accessibleDescription?.startsWith(SUGGESTION_LIST) ?: false
            })
            if (suggestionNodes.isEmpty()) return@execute false
            val lastSuggestionNode = suggestionNodes.last()
            terms.all { term -> lastSuggestionNode.find({ ctx -> ctx.foundText(term) }) != null }
        }
    }

    fun mostRecentSuggestionRowDoesNotContainsTerm(term: String): Boolean {
        return execute<Boolean> {
            val suggestionNodes = contextProvider().findAll({ ctx ->
                ctx.accessibleDescription?.startsWith(SUGGESTION_LIST) ?: false
            })
            if (suggestionNodes.isEmpty()) return@execute false
            val lastSuggestionNode = suggestionNodes.last()
            lastSuggestionNode.find({ ctx -> ctx.foundText(term) }) == null
        }
    }

    fun clickSuggestion(text: String) {
        val description = "$SUGGESTION_ITEM$text"
        await().atMost(ofSeconds(10)).until {
            try {
                execute<Boolean> {
                    val suggestionItem = contextProvider().find(description)
                    if (suggestionItem != null) {
                        suggestionItem.accessibleAction?.doAccessibleAction(0)
                        true
                    } else {
                        false
                    }
                }
            } catch (_: Exception) {
                false
            }
        }
    }

    fun suggestionsInMostRecentMessage(): List<String> {
        return execute<List<String>> {
            val suggestionNodes = contextProvider().findAll({ ctx ->
                ctx.accessibleDescription?.startsWith(SUGGESTION_LIST) ?: false
            })
            if (suggestionNodes.isEmpty()) return@execute emptyList()
            val lastSuggestionNode = suggestionNodes.last()
            lastSuggestionNode.findAll({ ctx ->
                ctx.accessibleDescription?.startsWith(SUGGESTION_ITEM) ?: false
            }).mapNotNull { ctx ->
                ctx.accessibleDescription?.removePrefix(SUGGESTION_ITEM)
            }
        }
    }

    fun numberOfSuggestionRows(): Int {
        return execute<Int> {
            contextProvider().findAll({ ctx ->
                ctx.accessibleDescription?.startsWith(SUGGESTION_LIST) ?: false
            }).size
        }
    }

    fun chatTextFieldContains(text: String): Boolean {
        return execute<Boolean> {
            val chatText = chatTextContext()?.accessibleName ?: ""
            chatText.contains(text)
        }
    }

    fun numberOfChatMessages(): Int =
        execute<Int> {
            val matcher = { context: AccessibleContext ->
                context.accessibleDescription?.startsWith(NUMBER_OF_CHAT_MESSAGES_) ?: false
            }
            contextProvider().find(matcher)
                ?.accessibleDescription
                ?.substring(NUMBER_OF_CHAT_MESSAGES_.length)
                ?.toIntOrNull() ?: 0
        }
}

fun AccessibleContext.foundText(text: String): Boolean =
    accessibleName?.contains(text) ?: false

fun AccessibleContext.isBotResponseForIndex(index: Int): Boolean =
    accessibleDescription == "$BOT$index"


