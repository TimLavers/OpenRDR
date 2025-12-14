package io.rippledown.server.action

import io.rippledown.model.ServerChatResult
import io.rippledown.server.ServerChatActionsInterface
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.KFunction

interface ServerAction {
    fun doIt(application: ServerChatActionsInterface, kbId: String? = null): ServerChatResult
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
        val className = "io.rippledown.server.action.$actionName"
        val kclass = try {
            Class.forName(className).asSubclass(ServerAction::class.java).kotlin
        } catch (e: Exception) {
            return null
        }
        val constructor = kclass.constructors.firstOrNull { it.hasSingleJsonObjectArgument() }
        return (constructor?.call(jsonObject))
    } catch (_: Exception) {
        return null;
    }
}
fun KFunction<*>.hasSingleJsonObjectArgument(): Boolean {
    return this.parameters.size == 1 && this.parameters[0].type.classifier == JsonObject::class
}
