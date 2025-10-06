package io.rippledown.kb.chat

import io.rippledown.kb.chat.action.ChatAction
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.isAccessible

@Serializable
data class ActionComment(
    val action: String,
    val message: String? = null,
    val debug: String? = null,
    val comment: String? = null,
    val replacementComment: String? = null,
    val reasons: List<String>? = null,
    val attributeMoved: String? = null, // todo refactor
    val destination: String? = null, // todo refactor
) {
    private val asMap = mutableMapOf<String, String>()

    init {
        if (message != null) asMap["message"] = message
        if (comment != null) asMap["comment"] = comment
        if (replacementComment != null) asMap["replacementComment"] = replacementComment
        if (reasons != null) asMap["reasons"] = reasons.joinToString(",")
        if (attributeMoved != null) asMap["attributeMoved"] = attributeMoved
        if (destination != null) asMap["destination"] = destination
    }

    fun createActionInstance(): ChatAction? {
        val className = "io.rippledown.kb.chat.action.${action}"
        try {
            val actionClass = Class<ChatAction>.forName(className).kotlin
            return actionClass.constructors.map { invokeConstructor(it as KFunction<ChatAction>) }.first { it != null }
        } catch (_: ClassNotFoundException) {
            return null
        }
    }

    fun invokeConstructor(fn: KFunction<ChatAction>): ChatAction? {
        if (fn.parameters.size != asMap.size) return null
        val paramMap = mutableMapOf<KParameter, String>()
        fn.parameters.forEach {
            val parameterName = it.name
            if (!asMap.containsKey(parameterName)) return null

            val parameterValue = asMap[parameterName]!!
            paramMap[it] = parameterValue
        }
       return fn.callBy(paramMap)
    }
}