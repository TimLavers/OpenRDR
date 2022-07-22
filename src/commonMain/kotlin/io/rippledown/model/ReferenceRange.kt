package io.rippledown.model

import kotlinx.serialization.Serializable

//ORD1
@Serializable
data class ReferenceRange(val lower: Float?, val upper: Float?) {
    init {
        check(lower != null || upper != null)
        if (lower != null && upper != null) {
            check(lower < upper)
        }
    }

    fun isHigh(v: Value): Boolean {
        v.real ?: return false
        if (upper == null) {
            return false
        }
         return v.real!! > upper
    }

    fun isLow(v: Value): Boolean {
        v.real ?: return false
        if (lower == null) {
            return false
        }
        return v.real!! < lower
    }

    fun isNormal(v: Value): Boolean {
        v.real ?: return false
        if (upper == null) {//so lower non null
            return v.real!! > lower!!
        }
        if (lower == null) {//so upper non null
            return v.real!! < upper
        }
        return v.real!! > lower && v.real!! < upper
    }
}