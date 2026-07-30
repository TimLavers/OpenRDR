package io.rippledown.persistence

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.rule.*
import kotlinx.serialization.json.Json
import kotlin.test.Test

class PersistentRuleAssignmentTest {
    private val weight = Attribute(1, "weight")
    private val height = Attribute(2, "height")
    private val diabetesStatus = Attribute(10, "Diabetes status", AttributeKind.DERIVED)
    private val bmi = Attribute(11, "BMI", AttributeKind.DERIVED)
    private val literalAssignment = AssignValue(diabetesStatus, Literal("diabetic"))
    private val formulaAssignment = AssignValue(
        bmi,
        Formula(
            Binary(
                Operator.DIVIDE,
                AttributeValue(weight),
                Binary(Operator.TIMES, AttributeValue(height), AttributeValue(height))
            )
        )
    )

    @Test
    fun `assignment defaults to null`() {
        PersistentRule(1, 0, null, setOf(2, 3)).assignment.shouldBeNull()
        PersistentRule().assignment.shouldBeNull()
    }

    @Test
    fun `construction from a rule carries the assignment`() {
        // Given a rule with an assignment
        val rule = Rule(7, Rule(0), null, emptySet(), mutableSetOf(), literalAssignment)

        // When a persistent rule is built from it
        val persistentRule = PersistentRule(rule)

        // Then the assignment is carried
        persistentRule.assignment shouldBe literalAssignment
        persistentRule.conclusionId.shouldBeNull()
    }

    @Test
    fun `serialization round trip with a literal assignment`() {
        // Given a persistent rule with a literal assignment
        val persistentRule = PersistentRule(1, 0, null, setOf(2), literalAssignment)

        // When it is serialized and deserialized
        val restored = Json.decodeFromString<PersistentRule>(Json.encodeToString(persistentRule))

        // Then it is unchanged
        restored shouldBe persistentRule
    }

    @Test
    fun `serialization round trip with a formula assignment`() {
        // Given a persistent rule with a formula assignment
        val persistentRule = PersistentRule(1, 0, null, emptySet(), formulaAssignment)

        // When it is serialized and deserialized
        val restored = Json.decodeFromString<PersistentRule>(Json.encodeToString(persistentRule))

        // Then it is unchanged
        restored shouldBe persistentRule
    }

    @Test
    fun `legacy JSON without an assignment field can be deserialized`() {
        // Given JSON written before the assignment field existed
        val legacy = """{"id":1,"parentId":0,"conclusionId":5,"conditionIds":[2,3]}"""

        // When it is deserialized
        val restored = Json.decodeFromString<PersistentRule>(legacy)

        // Then the assignment is null
        restored shouldBe PersistentRule(1, 0, 5, setOf(2, 3))
        restored.assignment.shouldBeNull()
    }

    @Test
    fun `assignment string round trip`() {
        // Given a persistent rule with an assignment
        val persistentRule = PersistentRule(1, 0, null, emptySet(), formulaAssignment)

        // When the assignment is written to and read from its string form
        val restored = PersistentRule.assignmentFromString(persistentRule.assignmentString())

        // Then it is unchanged
        restored shouldBe formulaAssignment
    }

    @Test
    fun `assignment string of a rule without an assignment is null`() {
        PersistentRule(1, 0, 5, setOf(2)).assignmentString().shouldBeNull()
        PersistentRule.assignmentFromString(null).shouldBeNull()
    }
}
