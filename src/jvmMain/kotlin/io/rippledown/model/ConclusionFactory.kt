package io.rippledown.model

interface ConclusionFactory {
    fun getOrCreate(text: String): Conclusion
    fun getById(id: Int): Conclusion?
}