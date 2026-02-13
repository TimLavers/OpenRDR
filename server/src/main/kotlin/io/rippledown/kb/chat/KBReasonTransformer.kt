package io.rippledown.kb.chat

import io.rippledown.chat.ReasonTransformation
import io.rippledown.chat.ReasonTransformer
import io.rippledown.chat.toExpressionTransformation
import io.rippledown.model.RDRCase
import io.rippledown.toJsonString

class KBReasonTransformer(
    private val case: RDRCase,
    private val ruleService: RuleService,
    private val modelResponder: ModelResponder
) : ReasonTransformer {

    override suspend fun transform(reason: String): ReasonTransformation {
        val result = ruleService.conditionForExpression(case, reason)
        val condition = result.condition
        if (condition != null) {
            ruleService.addConditionToCurrentRuleSession(condition)
            val cornerstoneStatus = ruleService.cornerstoneStatus()
            ruleService.sendCornerstoneStatus()
            modelResponder.response(cornerstoneStatus.toJsonString())
        }
        return result.toExpressionTransformation()
    }
}
