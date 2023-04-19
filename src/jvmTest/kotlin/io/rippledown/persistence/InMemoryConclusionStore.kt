package io.rippledown.persistence

import io.rippledown.model.Conclusion

class InMemoryConclusionStore: ConclusionStore {
    private val conclusionSet = mutableSetOf<Conclusion>()

    override fun all() = conclusionSet

    override fun create(text: String): Conclusion {
        val maxById = conclusionSet.maxByOrNull { it.id }
        val maxId = maxById?.id ?: 0
        val newConclusion = Conclusion(maxId + 1, text)
        conclusionSet.add(newConclusion)
        return newConclusion

    }

//    override fun getById( id: Int) = conclusionSet.firstOrNull { it.id == id }

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