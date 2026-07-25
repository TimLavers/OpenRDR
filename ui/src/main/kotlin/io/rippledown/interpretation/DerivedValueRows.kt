package io.rippledown.interpretation

import io.rippledown.model.caseview.DerivedValueInfo
import io.rippledown.model.diff.DerivedValueAddition
import io.rippledown.model.diff.DerivedValueChange
import io.rippledown.model.diff.DerivedValueRemoval
import io.rippledown.model.diff.DerivedValueReplacement

/**
 * How a derived attribute row should be highlighted, given the change the rule
 * session in progress is about to make.
 */
enum class DerivedValueHighlight { NONE, ADDED, REMOVED, REPLACED }

/**
 * A derived attribute row to draw, together with the pending change, if any,
 * that affects it.
 *
 * @param newValue for [DerivedValueHighlight.REPLACED], the value the rule
 *   being built will assign. It is shown after the current value, so that the
 *   user sees the value being replaced and its replacement together.
 */
data class DerivedValueRowState(
    val info: DerivedValueInfo,
    val highlight: DerivedValueHighlight = DerivedValueHighlight.NONE,
    val newValue: String? = null
)

/**
 * The rows the Derived attributes panel should draw for [derivedValues], given
 * the [change] the rule session in progress is about to make.
 *
 * A pending addition is not on the case yet, so a row for it is inserted in
 * name order. A pending removal or replacement applies to a row that is already
 * there.
 *
 * An addition naming an attribute that already has a value on the case is not
 * previewed at all, and the panel simply shows the current state. Such a
 * session should not arise, because the user is asked whether they meant to
 * replace the value before one is started; guessing that they meant a
 * replacement would show them a change they never asked for.
 *
 * @param ruleConditions the conditions of the rule being built, shown in the
 *   tooltip of a row that is being added, since it has no rule of its own yet.
 */
fun rowsToDisplay(
    derivedValues: List<DerivedValueInfo>,
    change: DerivedValueChange? = null,
    ruleConditions: List<String> = emptyList()
): List<DerivedValueRowState> {
    if (change == null) return derivedValues.map { DerivedValueRowState(it) }

    val existing = derivedValues.firstOrNull { it.name == change.attributeName }

    return when (change) {
        is DerivedValueRemoval -> derivedValues.map {
            if (it.name == change.attributeName) {
                DerivedValueRowState(it, DerivedValueHighlight.REMOVED)
            } else {
                DerivedValueRowState(it)
            }
        }

        is DerivedValueReplacement -> derivedValues.map {
            if (it.name == change.attributeName) {
                DerivedValueRowState(
                    info = it.copy(formula = change.newFormula, conditions = ruleConditions),
                    highlight = DerivedValueHighlight.REPLACED,
                    newValue = change.newValue
                )
            } else {
                DerivedValueRowState(it)
            }
        }

        is DerivedValueAddition -> if (existing == null) {
            val added = DerivedValueRowState(
                info = DerivedValueInfo(
                    name = change.attributeName,
                    value = change.value,
                    formula = change.formula,
                    conditions = ruleConditions
                ),
                highlight = DerivedValueHighlight.ADDED
            )
            (derivedValues.map { DerivedValueRowState(it) } + added).sortedBy { it.info.name }
        } else {
            derivedValues.map { DerivedValueRowState(it) }
        }
    }
}
