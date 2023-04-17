package io.rippledown.kb

import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.persistence.AttributeStore

class ConclusionManager() {
    private val textToConclusion = mutableMapOf<String, Conclusion>()

    init {
    }

    fun getOrCreate(text: String) : Conclusion {
        TODO()
//        return textToConclusion.computeIfAbsent(name) {
//
//        }
    }

    fun all(): Set<Conclusion> {
        return textToConclusion.values.toSet()
    }

    fun getById(id: Int): Conclusion {
        return textToConclusion.values.first { it.id == id }
    }
}