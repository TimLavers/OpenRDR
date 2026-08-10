package io.rippledown.kb

import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
import io.rippledown.model.rule.CommentTemplate
import io.rippledown.persistence.PersistentKB

/**
 * One-off conversion of a KB from conclusion rules to comment-attribute
 * rules: each conclusion becomes an auto-named COMMENT attribute whose
 * stored definition is a [CommentTemplate] of the conclusion's text and
 * variables, and each rule that gave the conclusion becomes a rule that
 * assigns the attribute by its definition. Rule ids, parents and
 * conditions are unchanged. The conclusion store is emptied, making the
 * conversion idempotent.
 *
 * This is applied to the KB fixtures in this project's resources (the
 * only KBs in the old format) before Phase 2 step 16 lands, and is then
 * deleted. See "Phase 2 — comments become derived attributes" in
 * documentation/design/repeat_inferencing.md.
 */
fun migrateConclusionsToCommentAttributes(persistentKB: PersistentKB) {
    val conclusionStore = persistentKB.conclusionStore()
    val conclusions = conclusionStore.all().sortedBy { it.id }
    if (conclusions.isEmpty()) return

    val attributeManager = AttributeManager(persistentKB.attributeStore())
    val definitionManager = DerivedDefinitionManager(persistentKB.derivedDefinitionStore(), attributeManager)
    val conclusionIdToAttribute = conclusions.associate { conclusion ->
        val attribute = attributeManager.createCommentAttribute()
        val variables = conclusion.variables.map { attributeManager.getById(it.attributeId) }
        definitionManager.store(attribute.id, CommentTemplate(conclusion.text, variables))
        conclusion.id to attribute
    }

    val ruleStore = persistentKB.ruleStore()
    ruleStore.all().filter { it.conclusionId != null }.forEach { persistentRule ->
        val attribute = conclusionIdToAttribute.getValue(persistentRule.conclusionId!!)
        ruleStore.update(
            persistentRule.copy(conclusionId = null, assignment = AssignValue(attribute, ByDefinition))
        )
    }

    conclusionStore.clear()
}
