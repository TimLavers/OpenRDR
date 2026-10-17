package io.rippledown.interpretation

import androidx.compose.runtime.staticCompositionLocalOf
import io.rippledown.model.condition.ConditionText
import java.util.concurrent.atomic.AtomicReference

/**
 * Optional, per-window observation of the conditions supplied to comment tooltips.
 * Cucumber installs this hook in its window's composition; normal clients do not.
 * The current-case comments panel publishes its actual rows after composition,
 * including both parts of a pending replacement. No popup or pointer is needed.
 * This observes tooltip content, not hover behaviour or popup visibility.
 */
class CommentTooltipTestHook {
    data class Tooltip(val text: String, val name: String, val conditions: List<ConditionText>)

    private val tooltips = AtomicReference<List<Tooltip>>(emptyList())

    fun update(rows: List<CommentRowState>) {
        tooltips.set(rows.flatMap { row ->
            listOfNotNull(row.comment, row.replacement).map {
                Tooltip(it.text, it.name, it.conditions.toList())
            }
        })
    }

    fun snapshot(): List<Tooltip> = tooltips.get()

    fun clear() = tooltips.set(emptyList())
}

val LocalCommentTooltipTestHook = staticCompositionLocalOf<CommentTooltipTestHook?> { null }
