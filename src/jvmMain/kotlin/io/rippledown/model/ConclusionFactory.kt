package io.rippledown.model

interface ConclusionFactory {
    fun create(text: String): Conclusion
}