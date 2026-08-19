package io.rippledown.kb

import io.rippledown.model.RDRCase
import io.rippledown.model.rule.*

/**
 * Starts a session for a rule adding the given comment, through the
 * low-level [RuleSessionManager.startRuleSession].
 *
 * [RuleSessionManager.startRuleSessionToAddComment] also records a pending
 * [io.rippledown.model.diff.Addition], which every [CornerstoneStatus] the
 * session produces then carries. Tests comparing a whole [CornerstoneStatus]
 * are not about that pending change, so they use this instead: it is the
 * direct analogue of the conclusion-era
 * `startRuleSession(case, ChangeTreeToAddConclusion(...))`.
 *
 * The comment attribute is registered with the KB and holds the text as its
 * definition, as in production, so that the case view can resolve it by id.
 */
fun RuleSessionManager.startRuleSessionToAssignComment(
    kb: KB,
    case: RDRCase,
    comment: String
): CornerstoneStatus {
    val template = CommentTemplate(comment)
    val attribute = kb.attributeManager.commentAttributes()
        .firstOrNull { kb.derivedDefinitionManager.definitionFor(it.id) == template }
        ?: kb.attributeManager.createCommentAttribute()
            .also { kb.derivedDefinitionManager.store(it.id, template) }
    return startRuleSession(case, ChangeTreeToAddAssignment(AssignValue(attribute, ByDefinition)))
}
