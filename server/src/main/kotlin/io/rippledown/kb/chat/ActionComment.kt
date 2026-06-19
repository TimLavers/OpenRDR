package io.rippledown.kb.chat

import io.rippledown.kb.chat.action.ChatAction
import io.rippledown.log.lazyLogger
import kotlinx.serialization.Serializable
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter


/**
 * @author Cascade AI
 */
@Serializable
data class ActionComment(
    val action: String,
    val message: String? = null,
    val debug: String? = null,
    val comment: String? = null,
    val replacementComment: String? = null,
    val reason: String? = null,
    val reasonId: Int? = null,
    val attributeMoved: String? = null,
    val destination: String? = null,
    val suggestions: List<String>? = null,
    val variables: List<io.rippledown.model.CommentVariable>? = null,
) {
    companion object {
        val logger = lazyLogger
    }

    fun createActionInstance(): ChatAction? {
        val className = "io.rippledown.kb.chat.action.$action"
        val kclass = try {
            Class.forName(className)
                .asSubclass(ChatAction::class.java)
                .kotlin
        } catch (e: Exception) {
            logger.error("Failed to create action instance from '$action': ${e.message}")
            return null
        }

        return kclass.constructors.firstNotNullOfOrNull { invokeConstructor(it) }
    }

    private fun invokeConstructor(fn: KFunction<ChatAction>): ChatAction? {
        val asMap = mutableMapOf<String, Any?>()
        if (message != null) asMap["message"] = message
        if (comment != null) asMap["comment"] = comment
        if (replacementComment != null) asMap["replacementComment"] = replacementComment
        if (reason != null) asMap["reason"] = reason
        if (reasonId != null) asMap["reasonId"] = reasonId
        if (attributeMoved != null) asMap["attributeMoved"] = attributeMoved
        if (destination != null) asMap["destination"] = destination
        if (variables != null) asMap["variables"] = variables

        val paramMap = mutableMapOf<KParameter, Any>()
        fn.parameters.forEach {
            val parameterName = it.name
            if (!asMap.containsKey(parameterName)) {
                if (it.isOptional) {
                    // Skip optional parameters with no value
                    return@forEach
                }
                return null
            }

            val parameterValue = asMap[parameterName]!!
            paramMap[it] = parameterValue
        }
       return fn.callBy(paramMap)
    }
}