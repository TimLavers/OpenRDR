package io.rippledown.model

import kotlinx.serialization.Serializable

/**
 * Distinguishes attributes whose values are supplied by the external
 * information system (e.g. a LIS) from those whose values are assigned
 * by the knowledge base.
 *
 * See documentation/design/repeat_inferencing.md.
 */
@Serializable
enum class AttributeKind {
    /**
     * Supplied by the external information system.
     */
    EXTERNAL,

    /**
     * Assigned by the knowledge base, shown in the Derived values panel.
     */
    DERIVED,

    /**
     * Assigned by the knowledge base, holds a report comment, shown in the
     * Comments panel.
     */
    COMMENT;

    /**
     * True for attributes whose values are assigned by the knowledge base.
     */
    fun isAssignedByKB() = this != EXTERNAL
}
