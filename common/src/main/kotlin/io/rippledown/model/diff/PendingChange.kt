package io.rippledown.model.diff

import kotlinx.serialization.Serializable

/**
 * The change the rule session currently in progress is about to make, so that
 * the panel it belongs to can preview it.
 *
 * A session makes exactly one change, either to the case's comments ([Diff]) or
 * to one of its derived attributes ([DerivedValueChange]). Carrying them as a
 * single value means a session cannot be described as making both, and the code
 * that displays a pending change has to say which kind it is handling.
 */
@Serializable
sealed interface PendingChange
