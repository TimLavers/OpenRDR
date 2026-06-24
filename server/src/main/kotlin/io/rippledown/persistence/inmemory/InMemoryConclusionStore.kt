package io.rippledown.persistence.inmemory

import io.rippledown.model.CommentVariable
import io.rippledown.model.Conclusion
import io.rippledown.persistence.ConclusionStore

class InMemoryConclusionStore: ConclusionStore {
    private val conclusionSet = mutableSetOf<Conclusion>()

    override fun all() = conclusionSet

    override fun create(text: String): Conclusion {
        return create(text, emptyList())
    }

    override fun create(text: String, variables: List<CommentVariable>): Conclusion {
        val maxById = conclusionSet.maxByOrNull { it.id }
        val maxId = maxById?.id ?: 0
        val newConclusion = Conclusion(maxId + 1, text, variables)
        conclusionSet.add(newConclusion)
        return newConclusion
    }

    override fun store(conclusion: Conclusion) {
        conclusionSet.add(conclusion)
    }

    override fun load(conclusions: Set<Conclusion>) {
        require(conclusionSet.isEmpty()) {
            "Cannot load conclusions into a non-empty conclusion store."
        }
        conclusionSet.addAll(conclusions)
    }
}