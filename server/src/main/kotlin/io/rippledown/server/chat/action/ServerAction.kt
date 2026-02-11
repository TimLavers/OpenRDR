package io.rippledown.server.chat.action

import io.rippledown.model.caseview.ViewableCase
import io.rippledown.server.ServerChatActionsInterface
import io.rippledown.server.chat.ModelResponder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.KFunction

interface ServerAction {
    suspend fun applyAction(
        application: ServerChatActionsInterface,
        kbId: String? = null,
        currentCase: ViewableCase? = null,
        modelResponder: ModelResponder? = null
    ): String
}

fun extractAction(response: String): ServerAction? {
    try {
        val startBracket = response.indexOf('{')
        val endBracket = response.lastIndexOf('}')
        val jsonPart = response.substring(startBracket, endBracket + 1)
        val jsonObject = Json.parseToJsonElement(jsonPart).jsonObject
        val actionName = jsonObject["action"]?.toString()?.trim('"')
        if (actionName == null) {
            return null
        }
        val className = "io.rippledown.server.chat.action.$actionName"
        val kclass = try {
            Class.forName(className).asSubclass(ServerAction::class.java).kotlin
        } catch (e: Exception) {
            return null
        }
        // If there are no arguments apart from the action name, seek a no-args constructor
        if (jsonObject.size == 1) {
            val constructor = kclass.constructors.firstOrNull { it.isNoArgsConstructor() }
            if (constructor != null) return (constructor.call() as ServerAction)
        }
        val constructor = kclass.constructors.firstOrNull { it.hasSingleJsonObjectArgument() }
        return (constructor?.call(jsonObject))
    } catch (_: Exception) {
        return null;
    }
}

fun KFunction<*>.isNoArgsConstructor(): Boolean {
        if (this.parameters.isEmpty()) {
            return true
        }
    return false
}

fun KFunction<*>.hasSingleJsonObjectArgument(): Boolean {
    return this.parameters.size == 1 && this.parameters[0].type.classifier == JsonObject::class
}
