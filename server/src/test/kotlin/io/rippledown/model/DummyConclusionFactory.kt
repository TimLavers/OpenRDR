package io.rippledown.model

import io.rippledown.kb.ConclusionProvider

class DummyConclusionFactory : ConclusionProvider {
    private val idToConclusion = mutableMapOf<Int, Conclusion>()

    override fun getOrCreate(text: String): Conclusion = getOrCreate(text, emptyList())

    override fun getOrCreate(text: String, variables: List<CommentVariable>): Conclusion {
        val existing = idToConclusion.values.firstOrNull { it.text == text && it.variables == variables }
        if (existing != null) {
            return existing
        }
        val newId = (idToConclusion.keys.maxOrNull() ?: 0) + 1
        val newConclusion = Conclusion(newId, text, variables)
        idToConclusion[newId] = newConclusion
        return newConclusion
    }

    override fun getById(id: Int) = idToConclusion[id]!!
}