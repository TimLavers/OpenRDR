package io.rippledown.kb

/** The latest rule has not been reviewed by this user, or changed after their preview. */
class UndoRuleRefusedException(override val message: String) : IllegalStateException(message)
