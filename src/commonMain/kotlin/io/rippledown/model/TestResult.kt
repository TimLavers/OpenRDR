package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class TestResult(val value: Value, val referenceRange: ReferenceRange?, val units: String?) {
    constructor(resultValue: String) : this(Value(resultValue), null, null)
}