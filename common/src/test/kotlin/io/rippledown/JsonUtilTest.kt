package io.rippledown

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class JsonUtilTest {

    @Test
    fun `should extract single JSON fragment`() {
        // Given
        val response = """{"action": "CommitRule"}"""

        // When
        val fragments = extractJsonFragments(response)

        // Then
        fragments shouldHaveSize 1
        fragments[0] shouldBe """{"action": "CommitRule"}"""
    }

    @Test
    fun `should extract multiple JSON fragments`() {
        // Given
        val response = """
            {"action": "ExemptCornerstone"}
            
            {"action": "CommitRule"}
        """.trimIndent()

        // When
        val fragments = extractJsonFragments(response)

        // Then
        fragments shouldHaveSize 2
        fragments[0] shouldBe """{"action": "ExemptCornerstone"}"""
        fragments[1] shouldBe """{"action": "CommitRule"}"""
    }

    @Test
    fun `should extract JSON fragments with additional properties`() {
        // Given
        val response = """
            {"action": "UserAction", "message": "Please confirm"}
            {"action": "CommitRule", "reason": "Rule is complete"}
        """.trimIndent()

        // When
        val fragments = extractJsonFragments(response)

        // Then
        fragments shouldHaveSize 2
        fragments[0] shouldBe """{"action": "UserAction", "message": "Please confirm"}"""
        fragments[1] shouldBe """{"action": "CommitRule", "reason": "Rule is complete"}"""
    }

    @Test
    fun `should handle empty response`() {
        // Given
        val response = ""

        // When
        val fragments = extractJsonFragments(response)

        // Then
        fragments shouldHaveSize 0
    }

    @Test
    fun `should handle whitespace-only response`() {
        // Given
        val response = "   \n\t  "

        // When
        val fragments = extractJsonFragments(response)

        // Then
        fragments shouldHaveSize 0
    }

    @Test
    fun `should handle nested objects in JSON`() {
        // Given
        val response = """
            {"action": "UserAction", "message": "Test", "debug": {"level": "info", "details": "test"}}
            {"action": "CommitRule"}
        """.trimIndent()

        // When
        val fragments = extractJsonFragments(response)

        // Then
        fragments shouldHaveSize 2
        fragments[0] shouldBe """{"action": "UserAction", "message": "Test", "debug": {"level": "info", "details": "test"}}"""
        fragments[1] shouldBe """{"action": "CommitRule"}"""
    }

    @Test
    fun `should handle JSON with arrays containing objects`() {
        // Given
        val response = """
            {"action": "UserAction", "data": [{"item": "value1"}, {"item": "value2"}]}
            {"action": "CommitRule"}
        """.trimIndent()

        // When
        val fragments = extractJsonFragments(response)

        // Then
        fragments shouldHaveSize 2
        fragments[0] shouldBe """{"action": "UserAction", "data": [{"item": "value1"}, {"item": "value2"}]}"""
        fragments[1] shouldBe """{"action": "CommitRule"}"""
    }

    @Test
    fun `should not treat a comment variable in braces as a JSON fragment`() {
        // Given a prose response mentioning a comment variable
        val response = "insert a case value into a comment by writing an attribute name in braces, e.g. {TSH}"

        // When
        val fragments = extractJsonFragments(response)

        // Then
        fragments shouldHaveSize 0
    }

    @Test
    fun `should extract a JSON fragment from a response that also contains a comment variable`() {
        // Given
        val response = """
            The comment can refer to {TSH}.
            {"action": "CommitRule"}
        """.trimIndent()

        // When
        val fragments = extractJsonFragments(response)

        // Then
        fragments shouldHaveSize 1
        fragments[0] shouldBe """{"action": "CommitRule"}"""
    }

    @Test
    fun `should extract an empty JSON object`() {
        // Given
        val response = "{}"

        // When
        val fragments = extractJsonFragments(response)

        // Then
        fragments shouldHaveSize 1
        fragments[0] shouldBe "{}"
    }

    @Test
    fun `should ignore a comment variable inside a JSON fragment`() {
        // Given
        val response = """{"action": "AddComment", "comment": "TSH is {TSH}."}"""

        // When
        val fragments = extractJsonFragments(response)

        // Then
        fragments shouldHaveSize 1
        fragments[0] shouldBe response
    }

    @Test
    fun `should handle JSON fragments with line breaks within objects`() {
        // Given
        val response = """
            {
                "action": "UserAction",
                "message": "Multi-line message"
            }
            {
                "action": "CommitRule"
            }
        """.trimIndent()

        // When
        val fragments = extractJsonFragments(response)

        // Then
        fragments shouldHaveSize 2
        fragments[0] shouldBe """
            {
                "action": "UserAction",
                "message": "Multi-line message"
            }""".trimIndent()
        fragments[1] shouldBe """
            {
                "action": "CommitRule"
            }""".trimIndent()
    }
}