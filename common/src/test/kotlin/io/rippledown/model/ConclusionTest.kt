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

    private val resolver: (Int) -> String = { id ->
        when (id) {
            1 -> "Wave"; 2 -> "Sun"; 10 -> "Glucose"; else -> "unknown"
        }
    }

    @Test
    fun truncatedText() {
        Conclusion(0, "Normal results.").truncatedText(resolver) shouldBe "Normal results."
        Conclusion(0, "Totally amazing results.").truncatedText(resolver) shouldBe "Totally amazing resu..."
    }

    @Test
    fun `truncatedText with single variable substitutes attribute name`() {
        // Given
        val conclusion = Conclusion(0, "The wave is ${'$'}{}", listOf(CommentVariable(1)))

        // Then
        conclusion.truncatedText(resolver) shouldBe "The wave is {Wave}"
    }

    @Test
    fun `truncatedText with variable at end substitutes attribute name`() {
        // Given
        val conclusion = Conclusion(0, "Quality: ${'$'}{}", listOf(CommentVariable(1)))

        // Then
        conclusion.truncatedText(resolver) shouldBe "Quality: {Wave}"
    }

    @Test
    fun `truncatedText with variable at start substitutes attribute name`() {
        // Given
        val conclusion = Conclusion(0, "${'$'}{} is the value", listOf(CommentVariable(1)))

        // Then - "{Wave} is the value" is 18 chars, no truncation
        conclusion.truncatedText(resolver) shouldBe "{Wave} is the value"
    }

    @Test
    fun `truncatedText with multiple variables substitutes and truncates`() {
        // Given
        val conclusion =
            Conclusion(0, "Quality: ${'$'}{}, temp: ${'$'}{}", listOf(CommentVariable(1), CommentVariable(2)))

        // Then - "Quality: {Wave}, temp: {Sun}" is 28 chars, truncates to 20
        conclusion.truncatedText(resolver) shouldBe "Quality: {Wave}, tem..."
    }

    @Test
    fun `truncatedText with long text and variable truncates normally`() {
        // Given
        val conclusion = Conclusion(
            0,
            "The wave quality is ${'$'}{} and the air temperature is ${'$'}{}",
            listOf(CommentVariable(1), CommentVariable(2))
        )

        // Then - truncates to 20 chars "The wave quality is " which doesn't end with "${"
        conclusion.truncatedText(resolver) shouldBe "The wave quality is ..."
    }

    @Test
    fun `truncatedText with variable in middle of long text truncates normally`() {
        // Given
        val conclusion =
            Conclusion(0, "Patient has glucose ${'$'}{} mmol/L which is concerning", listOf(CommentVariable(10)))

        // Then - "Patient has glucose {Glucose}" is 30 chars, truncates to 20
        conclusion.truncatedText(resolver) shouldBe "Patient has glucose ..."
    }

    @Test
    fun `truncatedText with adjacent variables substitutes both`() {
        // Given
        val conclusion = Conclusion(0, "Values: ${'$'}{}${'$'}{}", listOf(CommentVariable(1), CommentVariable(2)))

        // Then - "Values: {Wave}{Sun}" is 19 chars, no truncation
        conclusion.truncatedText(resolver) shouldBe "Values: {Wave}{Sun}"
    }

    @Test
    fun `truncatedText with text before variable truncates correctly`() {
        // Given
        val conclusion = Conclusion(0, "Very long text before ${'$'}{} placeholder", listOf(CommentVariable(1)))

        // Then - "Very long text before {Wave} placeholder" is 40 chars, truncates to 20
        conclusion.truncatedText(resolver) shouldBe "Very long text befor..."
    }

    @Test
    fun `truncatedText with text after variable truncates correctly`() {
        // Given
        val conclusion = Conclusion(0, "${'$'}{} with very long text after placeholder", listOf(CommentVariable(1)))

        // Then - "{Wave} with very long text after placeholder" is 43 chars, truncates to 20
        conclusion.truncatedText(resolver) shouldBe "{Wave} with very lon..."
    }

    @Test
    fun `truncatedText with variable at truncation boundary extends to include closing brace`() {
        // Given - raw text has ${} starting at position 18, so 20-char truncation ends with "${"
        val conclusion = Conclusion(0, "This is text with ${'$'}{} after", listOf(CommentVariable(1)))

        // Then - after substitution: "This is text with {Wave} after" (29 chars), truncates to 20
        conclusion.truncatedText(resolver) shouldBe "This is text with {W..."
    }

    // ==================== truncatedText with custom attributeNameById Tests ====================

    @Test
    fun `truncatedText with custom resolver substitutes single variable`() {
        // Given
        val conclusion = Conclusion(0, "The wave is ${'$'}{}", listOf(CommentVariable(1)))
        val customResolver: (Int) -> String = { id -> if (id == 1) "Wave" else "unknown" }

        // Then
        conclusion.truncatedText(customResolver) shouldBe "The wave is {Wave}"
    }

    @Test
    fun `truncatedText with custom resolver substitutes multiple variables`() {
        // Given
        val conclusion =
            Conclusion(0, "Quality: ${'$'}{}, temp: ${'$'}{}", listOf(CommentVariable(1), CommentVariable(2)))
        val customResolver: (Int) -> String = { id ->
            when (id) {
                1 -> "Wave"; 2 -> "Sun"; else -> "unknown"
            }
        }

        // Then - "Quality: {Wave}, temp: {Sun}" is 28 chars, truncates to 20
        conclusion.truncatedText(customResolver) shouldBe "Quality: {Wave}, tem..."
    }

    @Test
    fun `truncatedText with custom resolver returns unknown for unresolved attribute`() {
        // Given
        val conclusion = Conclusion(0, "The wave is ${'$'}{}", listOf(CommentVariable(999)))
        val unknownResolver: (Int) -> String = { _ -> "unknown" }

        // Then - "The wave is {unknown}" is 21 chars, truncates to 20
        conclusion.truncatedText(unknownResolver) shouldBe "The wave is {unknown..."
    }

    @Test
    fun `truncatedText with custom resolver returns unknown for partially unresolved attributes`() {
        // Given
        val conclusion =
            Conclusion(0, "Quality: ${'$'}{}, temp: ${'$'}{}", listOf(CommentVariable(1), CommentVariable(999)))
        val customResolver: (Int) -> String = { id -> if (id == 1) "Wave" else "unknown" }

        // Then - truncates to 20 chars
        conclusion.truncatedText(customResolver) shouldBe "Quality: {Wave}, tem..."
    }

    @Test
    fun `truncatedText with custom resolver does not call resolver when variables list is empty`() {
        // Given
        val conclusion = Conclusion(0, "Plain text with no variables")
        val neverCalledResolver: (Int) -> String = { _ -> error("Should not be called") }

        // Then - no variables, so resolver is not used, text is returned as-is (truncated)
        conclusion.truncatedText(neverCalledResolver) shouldBe "Plain text with no v..."
    }

    @Test
    fun `truncatedText with custom resolver substitutes variable at start`() {
        // Given
        val conclusion = Conclusion(0, "${'$'}{} is the value", listOf(CommentVariable(1)))
        val customResolver: (Int) -> String = { id -> if (id == 1) "Glucose" else "unknown" }

        // Then - "{Glucose} is the value" is 21 chars, truncates to 20
        conclusion.truncatedText(customResolver) shouldBe "{Glucose} is the val..."
    }

    @Test
    fun `truncatedText with custom resolver substitutes variable at end`() {
        // Given
        val conclusion = Conclusion(0, "The value is ${'$'}{}", listOf(CommentVariable(1)))
        val customResolver: (Int) -> String = { id -> if (id == 1) "Glucose" else "unknown" }

        // Then - "The value is {Glucose}" is 22 chars, truncates to 20
        conclusion.truncatedText(customResolver) shouldBe "The value is {Glucos..."
    }

    @Test
    fun `truncatedText with custom resolver handles adjacent variables`() {
        // Given
        val conclusion = Conclusion(0, "Values: ${'$'}{}${'$'}{}", listOf(CommentVariable(1), CommentVariable(2)))
        val customResolver: (Int) -> String = { id ->
            when (id) {
                1 -> "A"; 2 -> "B"; else -> "unknown"
            }
        }

        // Then - "Values: {A}{B}" is 16 chars, no truncation
        conclusion.truncatedText(customResolver) shouldBe "Values: {A}{B}"
    }

    @Test
    fun `truncatedText with custom resolver truncates long text after substitution`() {
        // Given
        val conclusion = Conclusion(
            0,
            "The wave quality is ${'$'}{} and the air temperature is ${'$'}{}",
            listOf(CommentVariable(1), CommentVariable(2))
        )
        val customResolver: (Int) -> String = { id ->
            when (id) {
                1 -> "Wave"; 2 -> "Sun"; else -> "unknown"
            }
        }

        // Then - substituted text is "The wave quality is {Wave} and the air temperature is {Sun}" (58 chars)
        // truncates to 20 chars "The wave quality is " which doesn't end with "${"
        conclusion.truncatedText(customResolver) shouldBe "The wave quality is ..."
    }

    @Test
    fun `truncatedText with custom resolver truncates at substitution boundary`() {
        // Given - designed so the 20-char truncation falls inside a {AttributeName} marker
        val conclusion = Conclusion(0, "This is text with ${'$'}{} after", listOf(CommentVariable(1)))
        val customResolver: (Int) -> String = { id -> if (id == 1) "Wave" else "unknown" }

        // Then - substituted text is "This is text with {Wave} after" (29 chars)
        // truncates to 20 chars "This is text with {W" which doesn't end with "${"
        conclusion.truncatedText(customResolver) shouldBe "This is text with {W..."
    }

    @Test
    fun `truncatedText with custom resolver handles more tokens than variables`() {
        // Given - text has 2 tokens but only 1 variable; second token should remain as ${}
        val conclusion = Conclusion(0, "A: ${'$'}{}, B: ${'$'}{}", listOf(CommentVariable(1)))
        val customResolver: (Int) -> String = { id -> if (id == 1) "Wave" else "unknown" }

        // Then - first token substituted, second remains as ${} (no variable to resolve it)
        // "A: {Wave}, B: ${}" is exactly 20 chars, no truncation
        conclusion.truncatedText(customResolver) shouldBe "A: {Wave}, B: ${'$'}{}"
    }

    @Test
    fun `truncatedText with custom resolver handles more variables than tokens`() {
        // Given - text has 1 token but 2 variables; extra variable is ignored
        val conclusion = Conclusion(0, "Only ${'$'}{} here", listOf(CommentVariable(1), CommentVariable(2)))
        val customResolver: (Int) -> String = { id ->
            when (id) {
                1 -> "Wave"; 2 -> "Sun"; else -> "unknown"
            }
        }

        // Then - only the first variable is used
        conclusion.truncatedText(customResolver) shouldBe "Only {Wave} here"
    }

    @Test
    fun `truncatedText with custom resolver returns plain text for conclusion without variables`() {
        // Given
        val conclusion = Conclusion(0, "Normal results.")
        val unusedResolver: (Int) -> String = { _ -> "unused" }

        // Then
        conclusion.truncatedText(unusedResolver) shouldBe "Normal results."
    }

    @Test
    fun `truncatedText with custom resolver truncates long plain text without variables`() {
        // Given
        val conclusion = Conclusion(0, "Totally amazing results.")
        val unusedResolver: (Int) -> String = { _ -> "unused" }

        // Then - no variables, so resolver is not used, normal truncation applies
        conclusion.truncatedText(unusedResolver) shouldBe "Totally amazing resu..."
    }

    @Test
    fun `truncatedText with custom resolver handles single-char attribute name`() {
        // Given
        val conclusion = Conclusion(0, "Value: ${'$'}{}", listOf(CommentVariable(1)))
        val customResolver: (Int) -> String = { id -> if (id == 1) "X" else "unknown" }

        // Then
        conclusion.truncatedText(customResolver) shouldBe "Value: {X}"
    }

    @Test
    fun `truncatedText with custom resolver handles long attribute name`() {
        // Given
        val conclusion = Conclusion(0, "Value: ${'$'}{}", listOf(CommentVariable(1)))
        val customResolver: (Int) -> String = { id -> if (id == 1) "VeryLongAttributeName" else "unknown" }

        // Then - "Value: {VeryLongAttributeName}" is 31 chars, truncates to 20
        conclusion.truncatedText(customResolver) shouldBe "Value: {VeryLongAttr..."
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