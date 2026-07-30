package io.rippledown.kb

import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.persistence.AttributeStore

typealias AttributeProvider = EntityProvider<Attribute>

class AttributeManager(private val attributeStore: AttributeStore): AttributeProvider {
    private val nameToAttribute = mutableMapOf<String, Attribute>()

    init {
        attributeStore.all().forEach {
            nameToAttribute[it.name] = it
        }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun getOrCreate(name: String): Attribute {
        return nameToAttribute.computeIfAbsent(name) {
            attributeStore.create(name)
        }
    }

    /**
     * Get the attribute with the given name, creating it with the given kind
     * if it does not exist. If an attribute with the name exists but has a
     * different kind, an exception is thrown: an attribute's kind is fixed
     * at creation.
     *
     * KB-assigned attributes (derived and comment) are also rejected if their
     * name matches an existing attribute name ignoring case, so that users
     * cannot define a derived attribute whose name differs only in case from
     * one that already exists.
     */
    fun getOrCreate(name: String, kind: AttributeKind): Attribute {
        val existing = nameToAttribute[name]
        if (existing != null) {
            require(existing.kind == kind) {
                "An attribute with name $name already exists with kind ${existing.kind}, not $kind."
            }
            return existing
        }
        if (kind.isAssignedByKB()) {
            val conflicting = nameToAttribute.entries
                .find { it.key.equals(name, ignoreCase = true) }
                ?.value
            if (conflicting != null) {
                error("An attribute with name \"${conflicting.name}\" already exists. Choose a different name.")
            }
        }
        return nameToAttribute.computeIfAbsent(name) {
            attributeStore.create(name, kind)
        }
    }

    fun byName(name: String): Attribute? = nameToAttribute[name]

    fun all(): Set<Attribute> {
        return nameToAttribute.values.toSet()
    }

    override fun getById(id: Int): Attribute {
        return nameToAttribute.values.first { it.id == id }
    }
}