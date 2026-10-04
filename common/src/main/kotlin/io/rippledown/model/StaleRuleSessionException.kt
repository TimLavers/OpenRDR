package io.rippledown.model

private const val SESSION_CANCELLED = "The rule session has been cancelled; please look at the case again."

fun interpretationChangedMessage(caseName: String) =
    "The interpretation of $caseName changed while you were building this rule. $SESSION_CANCELLED"

fun cornerstonesChangedMessage() = "The cornerstones changed while you were building this rule. $SESSION_CANCELLED"

/**
 * A rule session's commit was refused because another user's commit changed what
 * the session was built against. The session has been cancelled. Thrown by the
 * server and, over REST, reconstructed by the client from a 409 response.
 * See documentation/design/concurrent_users_revalidation.md.
 */
class StaleRuleSessionException(override val message: String) : RuntimeException(message)
