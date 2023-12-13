package io.rippledown.model

import io.rippledown.kb.ConclusionProvider

class DummyConclusionFactory : ConclusionProvider {
    private val idToConclusion = mutableMapOf<Int, Conclusion>()

    override fun getOrCreate(text: String): Conclusion {
        val existing = idToConclusion.values.firstOrNull { it.text == text }
        if (existing != null) {
            return existing
        }
        val newId = (idToConclusion.keys.maxOrNull() ?: 0) + 1
        val newConclusion = Conclusion(newId, text)
        idToConclusion[newId] = newConclusion
        return newConclusion
    }

    override fun getById(id: Int) = idToConclusion[id]!!
}