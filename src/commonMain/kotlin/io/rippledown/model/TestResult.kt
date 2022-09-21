package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class TestResult(val value: Value, val referenceRange: ReferenceRange?, val units: String?) {
    constructor(resultValue: String) : this(Value(resultValue), null, null)

    constructor(resultValue: String, referenceRange: ReferenceRange?, units: String?) : this(Value(resultValue), referenceRange, units )

    fun isHigh(): Boolean {
        referenceRange ?: return false
        return referenceRange.isHigh(value)
    }

    fun isLow(): Boolean {
        referenceRange ?: return false
        return referenceRange.isLow(value)
    }

    fun isNormal(): Boolean {
        referenceRange ?: return false
        return referenceRange.isNormal(value)
    }
}