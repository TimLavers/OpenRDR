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
 * @param newFormula for [DerivedValueHighlight.REPLACED], the definition the
 *   rule being built will give the attribute. It is shown after the current
 *   value, so that the user sees what is being replaced and its replacement
 *   together.
 */
data class DerivedValueRowState(
    val info: DerivedValueInfo,
    val highlight: DerivedValueHighlight = DerivedValueHighlight.NONE,
    val newFormula: String? = null
)

/**
 * The rows the Derived attributes panel should draw for [derivedValues], given
 * the [change] the rule session in progress is about to make.
 *
 * A pending addition is not on the case yet, so a row for it is inserted in
 * name order. A pending removal or replacement applies to a row that is already
 * there.
 *
 * A pending addition or replacement is shown as the definition the rule will
 * give the attribute, not as a value: no rule assigns a value to the case yet.
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
                    newFormula = change.newFormula.definitionText()
                )
            } else {
                DerivedValueRowState(it)
            }
        }

        is DerivedValueAddition -> if (existing == null) {
            val added = DerivedValueRowState(
                info = DerivedValueInfo(
                    name = change.attributeName,
                    value = change.formula.definitionText(),
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

/**
 * How the definition a rule is about to give an attribute is shown while the
 * change is pending. A formula is shown in braces, as a comment variable is,
 * marking it as something that is yet to be evaluated. A literal is shown as
 * the value itself, without braces or the quotes that mark it as a literal in
 * the expression, since it is the value the rule will assign whatever the case.
 */
internal fun String.definitionText() =
    if (length >= 2 && startsWith("\"") && endsWith("\"")) substring(1, length - 1) else "{$this}"
