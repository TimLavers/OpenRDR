package io.rippledown.model

import kotlinx.serialization.Serializable

@Serializable
data class TestResult(val value: Value, val date: Long, val referenceRange: ReferenceRange?, val units: String?) {
    constructor(resultValue: String, date: Long) : this(Value(resultValue), date,null, null)
}