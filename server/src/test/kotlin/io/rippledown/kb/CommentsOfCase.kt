package io.rippledown.kb

import io.rippledown.model.RDRCase

/**
 * The comments the knowledge base gives the case: the case is interpreted,
 * and each by-definition comment assignment resolved through the
 * attribute's definition, which is where a comment's text is held. See
 * "Phase 2 — comments become derived attributes" in
 * documentation/design/repeat_inferencing.md.
 */
fun KB.commentsFor(case: RDRCase): Set<String> {
    val materialised = ruleTree.materialise(case, definitionResolver)
    return materialised.interpretation
        .withResolvedDefinitions(definitionResolver)
        .commentTexts(materialised) { id -> runCatching { attributeManager.getById(id) }.getOrNull() }
}
