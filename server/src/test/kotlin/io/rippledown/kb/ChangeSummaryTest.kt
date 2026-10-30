package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.condition.isHigh
import io.rippledown.model.condition.isLow
import io.rippledown.model.rule.*
import kotlin.test.Test

class ChangeSummaryTest {
    private val glucose = Attribute(1, "Glucose", AttributeKind.EXTERNAL)
    private val tsh = Attribute(2, "TSH", AttributeKind.EXTERNAL)
    private val comment = Attribute(3, "C1", AttributeKind.COMMENT)
    private val otherComment = Attribute(4, "C2", AttributeKind.COMMENT)
    private val bmi = Attribute(5, "BMI", AttributeKind.DERIVED)
    private val root = Rule(0)

    private val describe: (AssignValue) -> String = {
        when (it.attribute) {
            comment -> "Go to the beach."
            otherComment -> "Stay home."
            else -> it.asText()
        }
    }

    @Test
    fun `a rule adding a comment`() {
        // Given
        val rule =
            Rule(1, root, setOf(isHigh(10, glucose), isLow(11, tsh)), assignment = AssignValue(comment, Literal("x")))

        // When
        val summary = rulesAddedSummary(rule, describe)

        // Then
        summary shouldBe "Added comment \"Go to the beach.\" when Glucose is high, TSH is low."
    }

    @Test
    fun `a rule with no conditions`() {
        // Given
        val rule = Rule(1, root, setOf(), assignment = AssignValue(comment, Literal("x")))

        // When
        val summary = rulesAddedSummary(rule, describe)

        // Then
        summary shouldBe "Added comment \"Go to the beach.\" with no conditions."
    }

    @Test
    fun `a condition the user named is shown by its phrase`() {
        // Given
        val rule =
            Rule(1, root, setOf(isHigh(10, glucose, "sugar up")), assignment = AssignValue(comment, Literal("x")))

        // When
        val summary = rulesAddedSummary(rule, describe)

        // Then
        summary shouldBe "Added comment \"Go to the beach.\" when sugar up."
    }

    @Test
    fun `a rule assigning a value`() {
        // Given
        val rule = Rule(1, root, setOf(isHigh(10, glucose)), assignment = AssignValue(bmi, Formula(Num(2.0))))

        // When
        val summary = rulesAddedSummary(rule, describe)

        // Then
        summary shouldBe "Assigned BMI = 2 when Glucose is high."
    }

    @Test
    fun `a rule removing a comment`() {
        // Given
        val parent = Rule(1, root, setOf(isHigh(10, glucose)), assignment = AssignValue(comment, Literal("x")))
        val rule = Rule(2, parent, setOf(isLow(11, tsh)))

        // When
        val summary = rulesAddedSummary(rule, describe)

        // Then
        summary shouldBe "Removed comment \"Go to the beach.\" when TSH is low."
    }

    @Test
    fun `a rule retracting a value`() {
        // Given
        val parent = Rule(1, root, setOf(isHigh(10, glucose)), assignment = AssignValue(bmi, Formula(Num(2.0))))
        val rule = Rule(2, parent, setOf(isLow(11, tsh)))

        // When
        val summary = rulesAddedSummary(rule, describe)

        // Then
        summary shouldBe "Retracted BMI = 2 when TSH is low."
    }

    @Test
    fun `a rule replacing a comment`() {
        // Given
        val parent = Rule(1, root, setOf(isHigh(10, glucose)), assignment = AssignValue(comment, Literal("x")))
        val rule = Rule(2, parent, setOf(isLow(11, tsh)), assignment = AssignValue(otherComment, Literal("y")))

        // When
        val summary = rulesAddedSummary(rule, describe)

        // Then
        summary shouldBe "Replaced comment \"Go to the beach.\" with \"Stay home.\" when TSH is low."
    }

    @Test
    fun `a rule replacing a value`() {
        // Given
        val parent = Rule(1, root, setOf(isHigh(10, glucose)), assignment = AssignValue(bmi, Formula(Num(2.0))))
        val rule = Rule(2, parent, setOf(isLow(11, tsh)), assignment = AssignValue(bmi, Formula(Num(3.0))))

        // When
        val summary = rulesAddedSummary(rule, describe)

        // Then
        summary shouldBe "Replaced BMI = 2 with BMI = 3 when TSH is low."
    }

    @Test
    fun `undoing reverses the summary of what was added`() {
        rulesUndoneSummary("Added comment \"Go to the beach.\" when Glucose is high.") shouldBe
                "Undid: Added comment \"Go to the beach.\" when Glucose is high."
    }

    @Test
    fun `moving an attribute`() {
        attributeMovedSummary(glucose, tsh) shouldBe "Moved \"Glucose\" to the position of \"TSH\" in the case view."
    }

    @Test
    fun `deleting a case`() {
        caseDeletedSummary("Case 1") shouldBe "Deleted case \"Case 1\"."
    }

    @Test
    fun `deleting a case from a user list`() {
        caseDeletedSummary("Case 1", "Good ones") shouldBe "Deleted case \"Case 1\" from the \"Good ones\" list."
    }

    @Test
    fun `setting the description`() {
        descriptionSetSummary() shouldBe "Changed the description."
    }
}
