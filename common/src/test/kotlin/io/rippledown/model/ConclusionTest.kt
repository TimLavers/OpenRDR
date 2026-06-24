package io.rippledown.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.utils.randomString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ConclusionTest {

    @Test
    fun construction() {
        val conclusion = Conclusion(0,"Normal results.")
        conclusion.text shouldBe  "Normal results."
        conclusion.id shouldBe 0
    }

    @Test
    fun truncatedText() {
        Conclusion(0,"Normal results.").truncatedText() shouldBe "Normal results."
        Conclusion(0,"Totally amazing results.").truncatedText() shouldBe "Totally amazing resu..."
    }

    @Test
    fun jsonSerialisation() {
        val conclusion = Conclusion(1,"Normal results.")
        val sd = serializeDeserialize(conclusion)
        sd.id shouldBe conclusion.id
        sd.text shouldBe conclusion.text
        assertEquals(sd, conclusion)
    }

    @Test
    fun testEquality() {
        Conclusion(1, "Blah") shouldBe Conclusion(1, "Blah")
        Conclusion(1, "Blah") shouldBe Conclusion(1, "Whatever")
        Conclusion(1, "Blah") shouldNotBe Conclusion(2, "Whatever")
        Conclusion(1, "Blah") shouldNotBe Conclusion(2, "Blah")
    }

    @Test
    fun testHashCode() {
        Conclusion(1, "Blah").hashCode() shouldBe Conclusion(1, "Whatever").hashCode()
    }

    @Test
    fun `name cannot be blank`() {
        shouldThrow<IllegalStateException> {
            Conclusion(22,"")
        }.message shouldBe "Conclusions cannot be blank."
    }

    @Test
    fun `name must be less than 2049 characters in length`() {
        repeat(2047) {
            Conclusion(it, randomString(it + 1))
        }
        shouldThrow<IllegalStateException> {
            Conclusion(2049, randomString(2049))
        }.message shouldBe "Conclusions have maximum length 2048."
    }

    private fun serializeDeserialize(conclusion: Conclusion): Conclusion {
        val serialized = Json.encodeToString(conclusion)
        return Json.decodeFromString(serialized)
    }

    // ==================== Comment Variable Tests ====================

    @Test
    fun `CommentVariable construction`() {
        // Given
        val attributeId = 42

        // When
        val variable = CommentVariable(attributeId)

        // Then
        variable.attributeId shouldBe attributeId
    }

    @Test
    fun `CommentVariable equality`() {
        // Given
        val variable1 = CommentVariable(1)
        val variable2 = CommentVariable(1)
        val variable3 = CommentVariable(2)

        // Then
        variable1 shouldBe variable2
        variable1 shouldNotBe variable3
    }

    @Test
    fun `Conclusion with empty variables list is plain comment (back-compatible)`() {
        // Given
        val conclusion = Conclusion(1, "Normal results.", emptyList())

        // Then
        conclusion.variables shouldBe emptyList()
        conclusion.text shouldBe "Normal results."
    }

    @Test
    fun `Conclusion with variables serializes correctly`() {
        // Given
        val variables = listOf(CommentVariable(1), CommentVariable(2))
        val conclusion = Conclusion(1, "Patient ${'$'}{} has ${'$'}{} mmol/L", variables)

        // When
        val deserialized = serializeDeserialize(conclusion)

        // Then
        deserialized.id shouldBe conclusion.id
        deserialized.text shouldBe conclusion.text
        deserialized.variables shouldBe conclusion.variables
    }

    @Test
    fun `Conclusion equality still based on id only (with variables)`() {
        // Given
        val conclusion1 = Conclusion(1, "Template ${'$'}{}", listOf(CommentVariable(1)))
        val conclusion2 = Conclusion(1, "Different ${'$'}{}", listOf(CommentVariable(2)))
        val conclusion3 = Conclusion(2, "Template ${'$'}{}", listOf(CommentVariable(1)))

        // Then
        conclusion1 shouldBe conclusion2
        conclusion1 shouldNotBe conclusion3
    }

    @Test
    fun `Conclusion hashCode still based on id only (with variables)`() {
        // Given
        val conclusion1 = Conclusion(1, "Template ${'$'}{}", listOf(CommentVariable(1)))
        val conclusion2 = Conclusion(1, "Different ${'$'}{}", listOf(CommentVariable(2)))

        // Then
        conclusion1.hashCode() shouldBe conclusion2.hashCode()
    }

    // ==================== Render Tests ====================

    @Test
    fun `render plain comment (no variables) returns text with no unresolved ranges`() {
        // Given
        val conclusion = Conclusion(1, "Normal results.", emptyList())
        val case = createTestCase()
        val attributeById = { id: Int -> Attribute(id, "TestAttribute") }

        // When
        val rendered = conclusion.render(case, attributeById)

        // Then
        rendered.text shouldBe "Normal results."
        rendered.unresolvedRanges shouldBe emptyList()
    }

    @Test
    fun `render single variable with valid value substitutes correctly`() {
        // Given
        val template = "Glucose is ${'$'}{} mmol/L"
        val variables = listOf(CommentVariable(1))
        val conclusion = Conclusion(1, template, variables)
        val case = createTestCase(mapOf(Attribute(1, "Glucose") to "12"))
        val attributeById = { id: Int -> Attribute(id, "Glucose") }

        // When
        val rendered = conclusion.render(case, attributeById)

        // Then
        rendered.text shouldBe "Glucose is 12 mmol/L"
        rendered.unresolvedRanges shouldBe emptyList()
    }

    @Test
    fun `render multiple variables with valid values substitutes all`() {
        // Given
        val template = "Patient ${'$'}{} has glucose ${'$'}{} mmol/L"
        val variables = listOf(CommentVariable(1), CommentVariable(2))
        val conclusion = Conclusion(1, template, variables)
        val case = createTestCase(
            mapOf(
                Attribute(1, "Patient Name") to "John Doe",
                Attribute(2, "Glucose") to "12"
            )
        )
        val attributeById = { id: Int ->
            when (id) {
                1 -> Attribute(1, "Patient Name")
                2 -> Attribute(2, "Glucose")
                else -> null
            }
        }

        // When
        val rendered = conclusion.render(case, attributeById)

        // Then
        rendered.text shouldBe "Patient John Doe has glucose 12 mmol/L"
        rendered.unresolvedRanges shouldBe emptyList()
    }

    @Test
    fun `render variable with missing attribute uses marker and records unresolved range`() {
        // Given
        val template = "Glucose is ${'$'}{} mmol/L"
        val variables = listOf(CommentVariable(999))
        val conclusion = Conclusion(1, template, variables)
        val case = createTestCase()
        val attributeById = { id: Int -> null }

        // When
        val rendered = conclusion.render(case, attributeById)

        // Then
        rendered.text shouldBe "Glucose is {no value} mmol/L"
        rendered.unresolvedRanges.size shouldBe 1
    }

    @Test
    fun `render variable with blank value uses marker and records unresolved range`() {
        // Given
        val template = "Glucose is ${'$'}{} mmol/L"
        val variables = listOf(CommentVariable(1))
        val conclusion = Conclusion(1, template, variables)
        val case = createTestCase(mapOf(Attribute(1, "Glucose") to ""))
        val attributeById = { id: Int -> Attribute(id, "Glucose") }

        // When
        val rendered = conclusion.render(case, attributeById)

        // Then
        rendered.text shouldBe "Glucose is {Glucose: no value} mmol/L"
        rendered.unresolvedRanges.size shouldBe 1
    }

    @Test
    fun `render variable with no value for case uses marker and records unresolved range`() {
        // Given
        val template = "Glucose is ${'$'}{} mmol/L"
        val variables = listOf(CommentVariable(1))
        val conclusion = Conclusion(1, template, variables)
        val case = createTestCase(emptyMap()) // No values for any attribute
        val attributeById = { id: Int -> Attribute(id, "Glucose") }

        // When
        val rendered = conclusion.render(case, attributeById)

        // Then
        rendered.text shouldBe "Glucose is {Glucose: no value} mmol/L"
        rendered.unresolvedRanges.size shouldBe 1
    }

    @Test
    fun `render adjacent variables substitutes both correctly`() {
        // Given
        val template = "Values: ${'$'}{}${'$'}{}"
        val variables = listOf(CommentVariable(1), CommentVariable(2))
        val conclusion = Conclusion(1, template, variables)
        val case = createTestCase(
            mapOf(
                Attribute(1, "A") to "X",
                Attribute(2, "B") to "Y"
            )
        )
        val attributeById = { id: Int ->
            when (id) {
                1 -> Attribute(1, "A")
                2 -> Attribute(2, "B")
                else -> null
            }
        }

        // When
        val rendered = conclusion.render(case, attributeById)

        // Then
        rendered.text shouldBe "Values: XY"
        rendered.unresolvedRanges shouldBe emptyList()
    }

    @Test
    fun `render variable at start of string substitutes correctly`() {
        // Given
        val template = "${'$'}{} is the value"
        val variables = listOf(CommentVariable(1))
        val conclusion = Conclusion(1, template, variables)
        val case = createTestCase(mapOf(Attribute(1, "Test") to "42"))
        val attributeById = { id: Int -> Attribute(id, "Test") }

        // When
        val rendered = conclusion.render(case, attributeById)

        // Then
        rendered.text shouldBe "42 is the value"
        rendered.unresolvedRanges shouldBe emptyList()
    }

    @Test
    fun `render variable at end of string substitutes correctly`() {
        // Given
        val template = "The value is ${'$'}{}"
        val variables = listOf(CommentVariable(1))
        val conclusion = Conclusion(1, template, variables)
        val case = createTestCase(mapOf(Attribute(1, "Test") to "42"))
        val attributeById = { id: Int -> Attribute(id, "Test") }

        // When
        val rendered = conclusion.render(case, attributeById)

        // Then
        rendered.text shouldBe "The value is 42"
        rendered.unresolvedRanges shouldBe emptyList()
    }

    @Test
    fun `render mix of resolved and unresolved variables records only unresolved ranges`() {
        // Given
        val template = "Patient ${'$'}{} has glucose ${'$'}{} mmol/L"
        val variables = listOf(CommentVariable(1), CommentVariable(2))
        val conclusion = Conclusion(1, template, variables)
        val case = createTestCase(mapOf(Attribute(1, "Patient Name") to "John Doe"))
        val attributeById = { id: Int ->
            when (id) {
                1 -> Attribute(1, "Patient Name")
                2 -> Attribute(2, "Glucose")
                else -> null
            }
        }

        // When
        val rendered = conclusion.render(case, attributeById)

        // Then
        rendered.text shouldBe "Patient John Doe has glucose {Glucose: no value} mmol/L"
        rendered.unresolvedRanges.size shouldBe 1
    }

    // ==================== Helper Functions ====================

    private fun createTestCase(values: Map<Attribute, String> = emptyMap()): RDRCase {
        val builder = RDRCaseBuilder()
        values.forEach { (attribute, value) ->
            builder.addValue(attribute, 0, value)
        }
        return builder.build("testCase", 1)
    }
}