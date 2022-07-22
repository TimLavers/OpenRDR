package io.rippledown.model

import kotlinx.serialization.Serializable

//ORD1
interface ReferenceRange {
    fun isHigh(v: Value): Boolean
    fun isLow(v: Value): Boolean
    fun isNormal(v: Value): Boolean
}

@Serializable
data class ClosedClosed(val lower: Float, val upper: Float): ReferenceRange {
    init {
        check(lower < upper)
    }

    override fun isHigh(v: Value): Boolean {
        v.real ?: return false
        return v.real!! > upper
    }

    override fun isLow(v: Value): Boolean {
        v.real ?: return false
        return v.real!! < lower
    }

    override fun isNormal(v: Value): Boolean {
        v.real ?: return false
        return v.real!! > lower && v.real!! < upper
    }
}