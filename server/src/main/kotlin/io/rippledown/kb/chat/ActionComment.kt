package io.rippledown.kb.chat

import io.rippledown.kb.chat.action.Action
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
    val attributeName: String? = null,
    val newName: String? = null,
    val valueExpression: String? = null,
    val suggestions: List<String>? = null,
    val variables: List<ChatCommentVariable>? = null,
    val kbName: String? = null,
    val kind: String? = null,
    val description: String? = null,
) {
    companion object {
        val logger = lazyLogger
    }

    fun createActionInstance(): Action? {
        val className = "io.rippledown.kb.chat.action.$action"
        val kclass = try {
            Class.forName(className)
                .asSubclass(Action::class.java)
                .kotlin
        } catch (e: Exception) {
            logger.error("Failed to create action instance from '$action': ${e.message}", e)
            return null
        }

        return kclass.constructors.firstNotNullOfOrNull { invokeConstructor(it) }
    }

    private fun invokeConstructor(fn: KFunction<Action>): Action? {
        val asMap = mutableMapOf<String, Any?>()
        if (message != null) asMap["message"] = message
        if (comment != null) asMap["comment"] = comment
        if (replacementComment != null) asMap["replacementComment"] = replacementComment
        if (reason != null) asMap["reason"] = reason
        if (reasonId != null) asMap["reasonId"] = reasonId
        if (attributeMoved != null) asMap["attributeMoved"] = attributeMoved
        if (destination != null) asMap["destination"] = destination
        if (attributeName != null) asMap["attributeName"] = attributeName
        if (newName != null) asMap["newName"] = newName
        if (valueExpression != null) asMap["valueExpression"] = valueExpression
        if (variables != null) asMap["variables"] = variables
        if (kbName != null) asMap["kbName"] = kbName
        if (kind != null) asMap["kind"] = kind
        if (description != null) asMap["description"] = description

        val paramMap = mutableMapOf<KParameter, Any>()
        fn.parameters.forEach {
            val parameterName = it.name ?: return null
            if (!asMap.containsKey(parameterName)) {
                if (it.isOptional) {
                    // Skip optional parameters with no value
                    return@forEach
                }
                return null
            }

            paramMap[it] = asMap.getValue(parameterName) ?: return null
        }
       return fn.callBy(paramMap)
    }
}