package io.rippledown.server

import io.rippledown.kb.commentsFor

/**
 * Comment rule sessions driven by case id, as the endpoint's callers do.
 * Comments are comment attributes, so these delegate to the comment entry
 * points of the rule session manager. See "Phase 2 — comments become
 * derived attributes" in documentation/design/repeat_inferencing.md.
 */
fun KBEndpoint.startRuleSessionToAddComment(caseId: Long, comment: String) =
    session.ruleSessionManager.startRuleSessionToAddComment(case(caseId), comment)

fun KBEndpoint.startRuleSessionToRemoveComment(caseId: Long, comment: String) =
    session.ruleSessionManager.startRuleSessionToRemoveComment(case(caseId), comment)

fun KBEndpoint.startRuleSessionToReplaceComment(caseId: Long, comment: String, replacement: String) =
    session.ruleSessionManager.startRuleSessionToReplaceComment(case(caseId), comment, replacement)

/**
 * The comments the knowledge base gives the case with the given id.
 */
fun KBEndpoint.commentsForCase(caseId: Long) = kb.commentsFor(uninterpretedCase(caseId))
