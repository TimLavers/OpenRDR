package io.rippledown.kb

import io.rippledown.model.Conclusion
import io.rippledown.persistence.ConclusionStore

class ConclusionManager(private val conclusionStore: ConclusionStore) : ConclusionProvider {
    private val textToConclusion = mutableMapOf<String, Conclusion>()

    init {
        conclusionStore.all().forEach {
            textToConclusion[it.text] = it
        }
    }

    override fun getOrCreate(text: String) : Conclusion {
        return textToConclusion.computeIfAbsent(text) {
            conclusionStore.create(text)
        }
    }

    fun all(): Set<Conclusion> {
        return textToConclusion.values.toSet()
    }

    override fun getById(id: Int): Conclusion {
        return textToConclusion.values.first { it.id == id }
    }
}