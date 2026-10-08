package io.rippledown.server

import io.rippledown.kb.commentsFor
import io.rippledown.model.UserId

/**
 * The user whose rule session the endpoint tests drive.
 */
val TEST_USER = UserId("test-user")

/**
 * Comment rule sessions driven by case id, as the endpoint's callers do.
 * Comments are comment attributes, so these delegate to the comment entry
 * points of the rule session manager.
 */
fun KBEndpoint.startRuleSessionToAddComment(caseId: Long, comment: String) =
    session.ruleSessionManagerFor(TEST_USER).startRuleSessionToAddComment(case(caseId), comment)

fun KBEndpoint.startRuleSessionToRemoveComment(caseId: Long, comment: String) =
    session.ruleSessionManagerFor(TEST_USER).startRuleSessionToRemoveComment(case(caseId), comment)

fun KBEndpoint.startRuleSessionToReplaceComment(caseId: Long, comment: String, replacement: String) =
    session.ruleSessionManagerFor(TEST_USER).startRuleSessionToReplaceComment(case(caseId), comment, replacement)

/**
 * The comments the knowledge base gives the case with the given id.
 */
fun KBEndpoint.commentsForCase(caseId: Long) = kb.commentsFor(uninterpretedCase(caseId))
