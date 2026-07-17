package io.rippledown.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.utils.daysAgo
import io.rippledown.utils.defaultDate
import kotlin.test.Test

internal class RDRCaseDerivedValuesTest {
    private val glucose = Attribute(1, "Glucose")
    private val weight = Attribute(2, "weight")
    private val diabetesStatus = Attribute(10, "Diabetes status", AttributeKind.DERIVED)
    private val adviceComment = Attribute(11, "DiabetesAdvice", AttributeKind.COMMENT)

    @Test
    fun `a derived value is added to the latest episode`() {
        // Given a case with two episodes
        val case = with(RDRCaseBuilder()) {
            addValue(glucose, daysAgo(1), "11.4")
            addValue(glucose, defaultDate, "12.0")
            build("Fermi")
        }

        // When a derived value is assigned
        val withDerived = case.withDerivedValue(diabetesStatus, "diabetic")

        // Then the value is in the latest episode only
        withDerived.latestValue(diabetesStatus) shouldBe "diabetic"
        withDerived.values(diabetesStatus)!!.map { it.value.text } shouldBe listOf("", "diabetic")
        withDerived.numberOfEpisodes() shouldBe 2

        // And the external data is unchanged
        withDerived.latestValue(glucose) shouldBe "12.0"

        // And the original case is not modified
        case.attributes shouldBe setOf(glucose)
    }

    @Test
    fun `a comment value can be assigned like any other derived value`() {
        // Given a case
        val case = with(RDRCaseBuilder()) {
            addValue(glucose, defaultDate, "12.0")
            build("Fermi")
        }

        // When a comment value is assigned
        val withComment = case.withDerivedValue(adviceComment, "Diabetic diet advice given.")

        // Then the value is on the case
        withComment.latestValue(adviceComment) shouldBe "Diabetic diet advice given."
    }

    @Test
    fun `a derived value cannot be assigned to an external attribute`() {
        // Given a case
        val case = with(RDRCaseBuilder()) {
            addValue(glucose, defaultDate, "12.0")
            build("Fermi")
        }

        // When a derived value is assigned to an external attribute
        // Then the assignment is rejected
        shouldThrow<IllegalArgumentException> {
            case.withDerivedValue(weight, "93.0")
        }.message shouldBe "Derived values can only be assigned to KB-assigned attributes, but weight is EXTERNAL."
    }

    @Test
    fun `a derived value cannot be assigned to a case with no episodes`() {
        // Given a case with no data
        val case = RDRCaseBuilder().build("Empty")

        // When a derived value is assigned
        // Then the assignment is rejected
        shouldThrow<IllegalStateException> {
            case.withDerivedValue(diabetesStatus, "diabetic")
        }.message shouldBe "Cannot assign a derived value to a case with no episodes."
    }

    @Test
    fun `stripping derived values removes derived and comment values but not external ones`() {
        // Given a case with external, derived and comment values
        val case = with(RDRCaseBuilder()) {
            addValue(glucose, defaultDate, "12.0")
            addValue(weight, defaultDate, "93.0")
            build("Fermi")
        }
            .withDerivedValue(diabetesStatus, "diabetic")
            .withDerivedValue(adviceComment, "Diabetic diet advice given.")

        // When the derived values are stripped
        val stripped = case.withoutDerivedValues()

        // Then only the external data remains
        stripped.attributes shouldBe setOf(glucose, weight)
        stripped.latestValue(glucose) shouldBe "12.0"
        stripped.latestValue(weight) shouldBe "93.0"
    }

    @Test
    fun `stripping a case with no derived values leaves its data unchanged`() {
        // Given a case with external data only
        val case = with(RDRCaseBuilder()) {
            addValue(glucose, defaultDate, "12.0")
            build("Fermi")
        }

        // When the derived values are stripped
        val stripped = case.withoutDerivedValues()

        // Then the data is unchanged
        stripped.hasSameDataAs(case) shouldBe true
    }

    @Test
    fun `assigning then stripping is idempotent`() {
        // Given a case with a derived value
        val original = with(RDRCaseBuilder()) {
            addValue(glucose, defaultDate, "12.0")
            build("Fermi")
        }
        val withDerived = original.withDerivedValue(diabetesStatus, "diabetic")

        // When the derived values are stripped
        val stripped = withDerived.withoutDerivedValues()

        // Then the case data is as it was before the assignment
        stripped.hasSameDataAs(original) shouldBe true
    }

    @Test
    fun `case identity and interpretation are carried over`() {
        // Given a case with an id and an interpretation
        val case = with(RDRCaseBuilder()) {
            addValue(glucose, defaultDate, "12.0")
            build("Fermi", 42)
        }

        // When a derived value is assigned and then stripped
        val withDerived = case.withDerivedValue(diabetesStatus, "diabetic")
        val stripped = withDerived.withoutDerivedValues()

        // Then the case identity and interpretation are unchanged
        withDerived.caseId shouldBe case.caseId
        withDerived.interpretation shouldBeSameInstanceAs case.interpretation
        stripped.caseId shouldBe case.caseId
        stripped.interpretation shouldBeSameInstanceAs case.interpretation
    }

    @Test
    fun `reassigning a derived value replaces the previous one`() {
        // Given a case with a derived value
        val case = with(RDRCaseBuilder()) {
            addValue(glucose, defaultDate, "12.0")
            build("Fermi")
        }.withDerivedValue(diabetesStatus, "diabetic")

        // When the same attribute is assigned a different value
        val reassigned = case.withDerivedValue(diabetesStatus, "pre-diabetic")

        // Then the new value replaces the old
        reassigned.latestValue(diabetesStatus) shouldBe "pre-diabetic"
        reassigned.numberOfEpisodes() shouldBe 1
    }
}
