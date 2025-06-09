package io.rippledown.chat.conversation

import dev.shreyaspatil.ai.client.generativeai.Chat
import dev.shreyaspatil.ai.client.generativeai.type.FunctionCallPart
import dev.shreyaspatil.ai.client.generativeai.type.FunctionDeclaration
import dev.shreyaspatil.ai.client.generativeai.type.Schema
import dev.shreyaspatil.ai.client.generativeai.type.content
import io.rippledown.chat.service.GeminiChatService
import io.rippledown.constants.chat.*
import io.rippledown.log.lazyLogger
import io.rippledown.model.RDRCase
import io.rippledown.stripEnclosingJson
import io.rippledown.toJsonString
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep
import kotlin.random.Random.Default.nextLong

interface ConversationService {
    suspend fun startConversation(case: RDRCase): String = ""
    suspend fun response(userMessage: String): String = ""
}
typealias ExpressionCheck = (String) -> Boolean

class Conversation(val expressionChecker: ExpressionCheck = { true }) : ConversationService {
    private val logger = lazyLogger
    private lateinit var chatService: GeminiChatService
    private lateinit var chat: Chat
    private val genericSystemInstruction = this::class.java.getResource("/system-instruction.md")?.readText()
        ?: throw IllegalStateException("Resource file 'system-instruction.md' not found in classpath")

    private val expressionCheck = FunctionDeclaration(
        name = "isExpressionValid",
        description = "Check if the expression is valid",
        parameters = listOf(
            Schema.str(
                name = "expression",
                description = "The expression to check for validity"
            )
        ),
        requiredParameters = listOf("expression")
    )

    override suspend fun startConversation(case: RDRCase): String {
        val caseSystemInstruction = insertValuesIntoPromptPlaceholders(case)
        chatService = GeminiChatService(caseSystemInstruction, listOf(expressionCheck))
        chat = retry {
            chatService.startChat()
        }
        return response("Please assist me with the report for this case.")
    }

    fun executeFunction(functionCall: FunctionCallPart): String {
        val expression = functionCall.args?.get("expression") ?: ""
        val isValid = expressionChecker(expression)
        logger.info("Function call: '${functionCall.name}' with args: '$expression', isValid: $isValid")
        return "'$expression' is valid?: ${isValid}"
    }

    override suspend fun response(userMessage: String) =
        runBlocking {
            val response = chat.sendMessage(userMessage)
            println("1. Function calls: ${response.functionCalls}")
            println("1. Text response: ${response.text}")

            response.functionCalls.let { functionCalls ->
                if (functionCalls.isNotEmpty()) {
                    val functionResult = executeFunction(functionCalls[0])
                    val prompt2 = content {
                        text("The function result is: $functionResult")
                    }
                    val response2 = chat.sendMessage(prompt2)
                    println("2. Function calls: ${response2.functionCalls}")
                    println("2. Text response: ${response2.text}")
                    response2.text ?: "No text response after function execution"
                } else {
                    response.text ?: "No function call or text response"
                }
            }
        }.stripEnclosingJson()

    private fun insertValuesIntoPromptPlaceholders(case: RDRCase): String = genericSystemInstruction
        .replace("{{case_json}}", case.toJsonString())
        .replace("{{PLEASE_CONFIRM}}", PLEASE_CONFIRM)
        .replace("{{ADD}}", ADD)
        .replace("{{REMOVE}}", REMOVE)
        .replace("{{REPLACE}}", REPLACE)
        .replace("{{NO_COMMENTS}}", NO_COMMENTS)
        .replace("{{EXISTING_COMMENTS}}", EXISTING_COMMENTS)
        .replace("{{WOULD_YOU_LIKE}}", WOULD_YOU_LIKE)
        .replace("{{ADD_A_COMMENT}}", ADD_A_COMMENT)
        .replace("{{REMOVE_A_COMMENT}}", REMOVE_A_COMMENT)
        .replace("{{REPLACE_A_COMMENT}}", REPLACE_A_COMMENT)
        .replace("{{WHAT_COMMENT}}", WHAT_COMMENT)
        .replace("{{ANY_CONDITIONS}}", ANY_CONDITIONS)
        .replace("{{START}}", START_ACTION)
        .replace("{{STOP}}", STOP_ACTION)
        .replace("{{DEBUG}}", DEBUG_ACTION)
        .replace("{{USER}}", USER_ACTION)
        .replace("{{ADD}}", ADD_ACTION)
        .replace("{{REMOVE}}", REMOVE_ACTION)
        .replace("{{REPLACE}}", REPLACE_ACTION)
}

/**
 * Retry when receiving the 503 error from the API due to rate limiting.
 */
object Retry

fun <T> retry(
    maxRetries: Int = 10,
    initialDelay: Long = 1_000,
    maxDelay: Long = 32_000,
    block: () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            Retry.lazyLogger.info("attempt $attempt failed. Waiting $currentDelay ms before retrying")
            sleep(currentDelay)
            currentDelay = (currentDelay * 2).coerceAtMost(maxDelay) + nextLong(0, 1_000)
        }
    }
    throw IllegalStateException("Max retries of $maxRetries reached")
}

