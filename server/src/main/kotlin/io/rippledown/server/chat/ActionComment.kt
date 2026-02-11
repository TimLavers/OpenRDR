package io.rippledown.server.chat

import io.rippledown.log.lazyLogger
import io.rippledown.server.chat.action.ServerAction
import kotlinx.serialization.Serializable
import kotlin.collections.get
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter


@Serializable
data class ActionComment(
    val action: String,
    val message: String? = null,
    val debug: String? = null,
    val comment: String? = null,
    val replacementComment: String? = null,
    val reason: String? = null,
    val attributeMoved: String? = null,
    val destination: String? = null,
    val kbName: String? = null,
) {
    companion object {
        val logger = lazyLogger
    }

    fun createActionInstance(): ServerAction? {
        val className = "io.rippledown.server.chat.action.$action"
        val kclass = try {
            Class.forName(className)
                .asSubclass(ServerAction::class.java)
                .kotlin
        } catch (e: Exception) {
            logger.error("Failed to create action instance from '$action'", e)
            return null
        }

        return kclass.constructors.firstNotNullOfOrNull { invokeConstructor(it) }
    }

    private fun invokeConstructor(fn: KFunction<ServerAction>): ServerAction? {
        val asMap = mutableMapOf<String, Any?>()
        if (message != null) asMap["message"] = message
        if (comment != null) asMap["comment"] = comment
        if (replacementComment != null) asMap["replacementComment"] = replacementComment
        if (reason != null) asMap["reason"] = reason
        if (attributeMoved != null) asMap["attributeMoved"] = attributeMoved
        if (destination != null) asMap["destination"] = destination
        if (kbName != null) asMap["kbName"] = kbName

        if (fn.parameters.size != asMap.size) return null
        val paramMap = mutableMapOf<KParameter, Any>()
        fn.parameters.forEach {
            val parameterName = it.name
            if (!asMap.containsKey(parameterName)) return null

            val parameterValue = asMap[parameterName]!!
            paramMap[it] = parameterValue
        }
       return fn.callBy(paramMap)
    }
}