package io.rippledown.model.caseview

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Whether a derived attribute obtained a value for the case. A valid
 * `false` from an AI Boolean is an [Evaluated] value; [NoInput] and
 * [Failed] assign nothing and are shown as diagnostics, never as values.
 * See documentation/design/ai_derived_attributes.md.
 */
@Serializable
sealed class DerivedValueStatus {
    @Serializable
    @SerialName("Evaluated")
    data object Evaluated : DerivedValueStatus()

    @Serializable
    @SerialName("NoInput")
    data class NoInput(val sourceName: String) : DerivedValueStatus()

    @Serializable
    @SerialName("Failed")
    data class Failed(val reason: String) : DerivedValueStatus()
}

/**
 * How an AI Boolean value was obtained, for display in the Derived
 * attributes panel. Independent of any provider SDK type.
 */
@Serializable
data class AiProvenance(
    val sourceName: String,
    val instructions: String,
    val evidence: String?,
    val evaluatedAt: Long,
    val model: String,
    val revision: String
)
