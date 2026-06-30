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

    /**
     * The existing conclusion with the given (internal-form) text, or null if none exists. Used to
     * resolve a comment the user is referring to (e.g. one being removed or replaced) back to the
     * conclusion already stored for it — including its comment variables — rather than minting a new,
     * variable-less conclusion that would not match the one in the case's interpretation.
     */
    fun findByText(text: String): Conclusion? = conclusionKeyMap.values.firstOrNull { it.text == text }

    override fun getById(id: Int): Conclusion {
        return conclusionKeyMap.values.first { it.id == id }
    }

    private data class ConclusionKey(val text: String, val variables: List<CommentVariable>)
}