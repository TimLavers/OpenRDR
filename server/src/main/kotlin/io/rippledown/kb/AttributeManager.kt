package io.rippledown.kb

import io.rippledown.log.lazyLogger
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.persistence.AttributeStore

typealias AttributeProvider = EntityProvider<Attribute>

/**
 * The longest name accepted for a KB-assigned attribute whose name is
 * proposed by the model rather than typed by the user. Names are meant to
 * be very concise labels, so a longer proposal is treated as a failure to
 * comply and the auto-generated name is used instead.
 */
const val MAX_PROPOSED_ATTRIBUTE_NAME_LENGTH = 20

class AttributeManager(private val attributeStore: AttributeStore): AttributeProvider {
    private val logger = lazyLogger
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

    /**
     * Create a comment attribute, named [proposedName] if that is a usable
     * name, and otherwise with an auto-generated name: `C1`, `C2`, …, using
     * the smallest index whose name is not already in use by an attribute of
     * any kind (ignoring case, consistent with the naming rules for
     * KB-assigned attributes). A proposed name comes from the model, so it is
     * only a suggestion: it is rejected, silently falling back to the
     * auto-generated name, if it is blank, not concise (see
     * [MAX_PROPOSED_ATTRIBUTE_NAME_LENGTH]), or already in use. See "Phase 2 —
     * comments become derived attributes" in
     * documentation/design/repeat_inferencing.md.
     */
    fun createCommentAttribute(proposedName: String? = null): Attribute {
        val name = proposedName?.trim()
        if (name != null && isUsableProposedName(name)) {
            return getOrCreate(name, AttributeKind.COMMENT)
        }
        if (name != null) {
            logger.info("Proposed comment attribute name \"$name\" is not usable, so it will be auto-named.")
        }
        val namesInUse = nameToAttribute.keys.map { it.lowercase() }.toSet()
        val index = generateSequence(1) { it + 1 }.first { "c$it" !in namesInUse }
        return getOrCreate("C$index", AttributeKind.COMMENT)
    }

    private fun isUsableProposedName(name: String) =
        name.isNotBlank() && name.length <= MAX_PROPOSED_ATTRIBUTE_NAME_LENGTH && !isNameInUse(name)

    /**
     * Whether any attribute, of any kind, has the given name, ignoring case.
     */
    fun isNameInUse(name: String) = nameToAttribute.keys.any { it.equals(name, ignoreCase = true) }

    /**
     * Rename the given attribute, which changes its name and nothing else:
     * everything that refers to an attribute does so by id. The new name is
     * refused if it is blank or in use by another attribute (ignoring case,
     * consistent with the naming rules for KB-assigned attributes); changing
     * only the case of the attribute's own name is allowed. See step 14 of
     * documentation/design/repeat_inferencing.md.
     */
    fun rename(attribute: Attribute, newName: String): Attribute {
        val name = newName.trim()
        require(name.isNotEmpty()) { "An attribute name cannot be blank." }
        val toRename = nameToAttribute.values.firstOrNull { it.id == attribute.id }
            ?: error("No attribute with name \"${attribute.name}\" exists.")
        val conflicting = nameToAttribute.values
            .firstOrNull { it.name.equals(name, ignoreCase = true) && it.id != toRename.id }
        if (conflicting != null) {
            error("An attribute with name \"${conflicting.name}\" already exists. Choose a different name.")
        }
        nameToAttribute.remove(toRename.name)
        toRename.name = name
        nameToAttribute[name] = toRename
        attributeStore.store(toRename)
        logger.info("Renamed attribute ${toRename.id} to \"$name\".")
        return toRename
    }

    /**
     * All the comment attributes.
     */
    fun commentAttributes(): Set<Attribute> =
        nameToAttribute.values.filter { it.kind == AttributeKind.COMMENT }.toSet()

    fun byName(name: String): Attribute? = nameToAttribute[name]

    fun all(): Set<Attribute> {
        return nameToAttribute.values.toSet()
    }

    override fun getById(id: Int): Attribute {
        return nameToAttribute.values.first { it.id == id }
    }
}