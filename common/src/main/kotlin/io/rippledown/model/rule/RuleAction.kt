package io.rippledown.model.rule

import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What a rule does when it fires. Two kinds for now:
 * [GiveConclusion] wraps the existing conclusion behaviour, and
 * [AssignValue] assigns the result of a [ValueExpression] to a derived
 * attribute. See documentation/design/repeat_inferencing.md.
 */
@Serializable
sealed class RuleAction

@Serializable
@SerialName("GiveConclusion")
data class GiveConclusion(val conclusion: Conclusion) : RuleAction()

@Serializable
@SerialName("AssignValue")
data class AssignValue(val attribute: Attribute, val expression: ValueExpression) : RuleAction() {
    init {
        require(attribute.kind.isAssignedByKB()) {
            "Values can only be assigned to KB-assigned attributes, but ${attribute.name} is ${attribute.kind}."
        }
    }

    fun asText() = "${attribute.name} = ${expression.asText()}"
}
