package io.rippledown.kb.chat

import io.rippledown.chat.Conversation.Companion.REASON_PARAMETER
import io.rippledown.chat.FunctionCallHandler
import io.rippledown.chat.ReasonTransformer
import io.rippledown.toJsonString

class ReasonTransformHandler(private val reasonTransformer: ReasonTransformer) : FunctionCallHandler {
    override suspend fun handle(args: Map<String, Any?>): String {
        val reason = args[REASON_PARAMETER]?.toString() ?: ""
        val transformation = reasonTransformer.transform(reason)
        val result = "'$reason' evaluation: ${transformation.toJsonString()}"
        val cornerstoneStatus = transformation.cornerstoneStatusJson
        return if (cornerstoneStatus != null) "$result\nCornerstone status: $cornerstoneStatus" else result
    }
}
