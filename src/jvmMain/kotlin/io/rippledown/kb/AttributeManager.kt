package io.rippledown.kb

import io.rippledown.model.Attribute
import io.rippledown.persistence.AttributeStore

typealias AttributeProvider = EntityProvider<Attribute>

class AttributeManager(private val attributeStore: AttributeStore): AttributeProvider {
    private val nameToAttribute = mutableMapOf<String, Attribute>()

    init {
        attributeStore.all().forEach {
            nameToAttribute[it.name] = it
        }
    }

    override fun forId(id: Int): Attribute {
        TODO("Not yet implemented")
    }

    fun getOrCreate(name: String) : Attribute {
        return nameToAttribute.computeIfAbsent(name) {
            attributeStore.create(name)
        }
    }

    fun all(): Set<Attribute> {
        return nameToAttribute.values.toSet()
    }

    fun getById(id: Int):Attribute {
        return nameToAttribute.values.first { it.id == id }
    }
}