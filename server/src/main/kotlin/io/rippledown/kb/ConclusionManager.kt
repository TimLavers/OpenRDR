package io.rippledown.kb

import io.rippledown.model.CommentVariable
import io.rippledown.model.Conclusion
import io.rippledown.persistence.ConclusionStore

class ConclusionManager(private val conclusionStore: ConclusionStore) : ConclusionProvider {
    private val conclusionKeyMap = mutableMapOf<ConclusionKey, Conclusion>()

    init {
        conclusionStore.all().forEach {
            conclusionKeyMap[ConclusionKey(it.text, it.variables)] = it
        }
    }

    override fun getOrCreate(text: String): Conclusion = getOrCreate(text, emptyList())

    override fun getOrCreate(text: String, variables: List<CommentVariable>): Conclusion {
        val key = ConclusionKey(text, variables)
        return conclusionKeyMap.computeIfAbsent(key) {
            conclusionStore.create(text, variables)
        }
    }

    fun all(): Set<Conclusion> {
        return conclusionKeyMap.values.toSet()
    }

    override fun getById(id: Int): Conclusion {
        return conclusionKeyMap.values.first { it.id == id }
    }

    private data class ConclusionKey(val text: String, val variables: List<CommentVariable>)
}