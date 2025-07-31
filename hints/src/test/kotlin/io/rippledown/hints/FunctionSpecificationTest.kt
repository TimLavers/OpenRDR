package io.rippledown.hints

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

class FunctionSpecificationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `should serialize FunctionSpecification with name and parameters`() {
        val spec = FunctionSpecification("add", listOf("a", "b"))
        val serialized = json.encodeToString(spec)
        serialized shouldBe """{"name":"add","parameters":["a","b"]}"""
    }

    @Test
    fun `should serialize FunctionSpecification with empty name and parameters`() {
        val spec = FunctionSpecification("", emptyList())
        val serialized = json.encodeToString(spec)
        serialized shouldBe """{"name":"","parameters":[]}"""
    }

    @Test
    fun `should deserialize FunctionSpecification with name and parameters`() {
        val jsonString = """{"name":"multiply","parameters":["x","y"]}"""
        val deserialized = json.decodeFromString<FunctionSpecification>(jsonString)
        deserialized shouldBe FunctionSpecification("multiply", listOf("x", "y"))
    }

    @Test
    fun `should deserialize FunctionSpecification with empty name and parameters`() {
        val jsonString = """{"name":"","parameters":[]}"""
        val deserialized = json.decodeFromString<FunctionSpecification>(jsonString)
        deserialized shouldBe FunctionSpecification("", emptyList())
    }

    @Test
    fun `should deserialize FunctionSpecification with no parameters`() {
        val jsonString = """{"name":"square"}"""
        val deserialized = json.decodeFromString<FunctionSpecification>(jsonString)
        deserialized shouldBe FunctionSpecification("square", emptyList())
    }

    @Test
    fun `should deserialize FunctionSpecification with single parameter`() {
        val jsonString = """{"name":"square","parameters":"x"}"""
        val deserialized = json.decodeFromString<FunctionSpecification>(jsonString)
        deserialized shouldBe FunctionSpecification("square", listOf("x"))
    }

    @Test
    fun `should deserialize FunctionSpecification with empty parameters as JsonPrimitive`() {
        val jsonString = """{"name":"noop","parameters":""}"""
        val deserialized = json.decodeFromString<FunctionSpecification>(jsonString)
        deserialized shouldBe FunctionSpecification("noop", emptyList())
    }

    @Test
    fun `should deserialize JSON with null parameter`() {
        //Given
        val jsonWithNullParameter = """{"name":"test","parameters":null}"""

        //When
        val deserialized = json.decodeFromString<FunctionSpecification>(jsonWithNullParameter)

        //Then
        deserialized shouldBe FunctionSpecification("test", emptyList())
    }

    @Test
    fun `should deserialize JSON with numeric parameter`() {
        //Given
        val jsonWithUnquotedParameter = """{"name":"test","parameters":123}"""

        //When
        val deserialized = json.decodeFromString<FunctionSpecification>(jsonWithUnquotedParameter)

        //Then
        deserialized shouldBe FunctionSpecification("test", listOf("123"))
    }

    @Test
    fun `should deserialize JSON with unquoted parameter`() {
        //Given
        val jsonString = """{"name":"test","parameters":abc}"""

        //When
        val deserialized = json.decodeFromString<FunctionSpecification>(jsonString)

        //Then
        deserialized shouldBe FunctionSpecification("test", listOf("abc"))
    }

    @Test
    fun `should deserialize JSON with quoted string parameter`() {
        //Given
        val jsonString = """{"name":"test","parameters":"\"abc\""}"""

        //When
        val deserialized = json.decodeFromString<FunctionSpecification>(jsonString)

        //Then
        deserialized shouldBe FunctionSpecification("test", listOf("\"abc\""))
    }

    @Test
    fun `should deserialize JSON with several quoted string parameters`() {
        //Given
        val jsonString = """{"name":"test","parameters":["\"abc\"","\"def\""]}"""

        //When
        val deserialized = json.decodeFromString<FunctionSpecification>(jsonString)

        //Then
        deserialized shouldBe FunctionSpecification("test", listOf("\"abc\"", "\"def\""))
    }

    @Test
    fun `should deserialize JSON with a mixture of quoted and unquoted string parameters`() {
        //Given
        val jsonString = """{"name":"test","parameters":["abc", "\"def\""]}"""

        //When
        val deserialized = json.decodeFromString<FunctionSpecification>(jsonString)

        //Then
        deserialized shouldBe FunctionSpecification("test", listOf("abc", "\"def\""))
    }

    @Test
    fun `should handle missing parameters field`() {
        val jsonString = """{"name":"test"}"""
        val deserialized = json.decodeFromString<FunctionSpecification>(jsonString)
        deserialized shouldBe FunctionSpecification("test", emptyList())
    }

    @Test
    fun `should handle missing name field`() {
        val jsonString = """{"parameters":["a","b"]}"""
        val deserialized = json.decodeFromString<FunctionSpecification>(jsonString)
        deserialized shouldBe FunctionSpecification("", listOf("a", "b"))
    }
}