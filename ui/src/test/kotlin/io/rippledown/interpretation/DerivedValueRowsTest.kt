package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.rippledown.model.caseview.DerivedValueInfo
import io.rippledown.model.diff.DerivedValueAddition
import io.rippledown.model.diff.DerivedValueRemoval
import io.rippledown.model.diff.DerivedValueReplacement
import kotlin.test.Test

class DerivedValueRowsTest {

    private val bmi = DerivedValueInfo(
        name = "BMI",
        value = "30.93",
        formula = "weight / height ^ 2",
        conditions = listOf("Weight is high")
    )
    private val risk = DerivedValueInfo(
        name = "Risk",
        value = "high",
        formula = "\"high\"",
        conditions = emptyList()
    )

    @Test
    fun `no change leaves every row unhighlighted`() {
        // Given derived values and no rule session in progress
        // When the rows are computed
        val rows = rowsToDisplay(listOf(bmi, risk))

        // Then nothing is highlighted and the values are untouched
        rows shouldBe listOf(DerivedValueRowState(bmi), DerivedValueRowState(risk))
    }

    @Test
    fun `an addition inserts a row in name order`() {
        // Given existing values either side of the one being added
        val alpha = risk.copy(name = "Alpha")
        val zeta = risk.copy(name = "Zeta")

        // When an addition for a name in between is applied
        val rows = rowsToDisplay(
            listOf(alpha, zeta),
            DerivedValueAddition("Mu", "\"2\""),
            ruleConditions = listOf("Sex is F")
        )

        // Then the new row is in name order and marked as added
        rows.map { it.info.name } shouldBe listOf("Alpha", "Mu", "Zeta")
        rows.map { it.highlight } shouldBe listOf(
            DerivedValueHighlight.NONE,
            DerivedValueHighlight.ADDED,
            DerivedValueHighlight.NONE
        )
    }

    @Test
    fun `an added row shows the formula in braces, with the rule conditions`() {
        // Given a rule session with a condition so far
        // When an addition is applied
        val rows = rowsToDisplay(
            emptyList(),
            DerivedValueAddition("BMI", "weight / height ^ 2"),
            ruleConditions = listOf("Weight is high")
        )

        // Then the row shows the formula the rule will give the attribute rather
        // than a value, since no rule assigns one yet
        rows.single().info shouldBe DerivedValueInfo(
            name = "BMI",
            value = "{weight / height ^ 2}",
            formula = "weight / height ^ 2",
            conditions = listOf("Weight is high")
        )
    }

    @Test
    fun `an added row shows a literal definition without braces`() {
        // Given a rule session assigning a literal
        // When the addition is applied
        val rows = rowsToDisplay(emptyList(), DerivedValueAddition("Risk", "\"high\""))

        // Then the literal is shown as the value it will assign, whatever the case
        rows.single().info.value shouldBe "high"
    }

    @Test
    fun `an addition for an attribute that already has a value is not previewed`() {
        // Given the attribute already has a value, which the user is asked about
        // before such a session can start
        // When an addition for it is applied anyway
        val rows = rowsToDisplay(listOf(bmi), DerivedValueAddition("BMI", "weight / height ^ 3"))

        // Then the current state is shown unchanged: no duplicate row, and no
        // replacement the user never asked for
        rows shouldBe listOf(DerivedValueRowState(bmi))
    }

    @Test
    fun `a removal highlights only the matching row`() {
        // Given two derived values
        // When one is being removed
        val rows = rowsToDisplay(listOf(bmi, risk), DerivedValueRemoval("BMI"))

        // Then only that row is marked, and no row is added or dropped
        rows.map { it.info.name } shouldBe listOf("BMI", "Risk")
        rows.map { it.highlight } shouldBe listOf(DerivedValueHighlight.REMOVED, DerivedValueHighlight.NONE)
    }

    @Test
    fun `a removal of a value the case does not have highlights nothing`() {
        // Given a removal naming an attribute with no value on this case
        // When the rows are computed
        val rows = rowsToDisplay(listOf(risk), DerivedValueRemoval("BMI"))

        // Then nothing is highlighted and no row is invented
        rows shouldBe listOf(DerivedValueRowState(risk))
    }

    @Test
    fun `a replacement keeps one row and previews the new definition`() {
        // Given a rule session replacing an existing assignment
        // When the rows are computed
        val rows = rowsToDisplay(
            listOf(bmi),
            DerivedValueReplacement("BMI", "weight / height ^ 3"),
            ruleConditions = listOf("Height is high")
        )

        // Then a single row shows the current value plus the definition replacing it
        rows.size shouldBe 1
        with(rows.single()) {
            highlight shouldBe DerivedValueHighlight.REPLACED
            info.value shouldBe "30.93"
            newFormula shouldBe "{weight / height ^ 3}"
            info.formula shouldBe "weight / height ^ 3"
            info.conditions shouldBe listOf("Height is high")
        }
    }

    @Test
    fun `a replacement leaves other rows alone`() {
        // Given another derived value that is not being changed
        // When a replacement is applied
        val rows = rowsToDisplay(listOf(bmi, risk), DerivedValueReplacement("BMI", "weight / height ^ 3"))

        // Then it is untouched
        rows.last() shouldBe DerivedValueRowState(risk)
    }
}
