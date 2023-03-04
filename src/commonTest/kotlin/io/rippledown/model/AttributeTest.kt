package io.rippledown.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

// ORD1
internal class AttributeTest {

    @Test //Attr-1
    fun construction() {
        val tsh = Attribute("TSH")
        assertEquals(tsh.name, "TSH")
    }

    @Test
    fun jsonSerialisation() {
        val tsh = Attribute("TSH")
        val sd = serializeDeserialize(tsh)
        assertEquals(sd, tsh)
    }

    @Test //Attr-2
    fun nameNotBlank() {
        shouldThrow<IllegalStateException> {
            Attribute("")
        }.message shouldBe "Attribute names cannot be blank."
    }

    @Test //Attr-3
    fun `name must be less than 256 characters in length`() {
        fun randomString(length: Int): String {
            //https://stackoverflow.com/questions/46943860/idiomatic-way-to-generate-a-random-alphanumeric-string-in-kotlin
            val alphabet: List<Char> = ('a'..'z') + ('A'..'Z')
            return List(length) { alphabet.random() }.joinToString("")
        }
        repeat(254) {
            Attribute(randomString(it + 1))
        }
        shouldThrow<IllegalStateException> {
            Attribute(randomString(256))
        }.message shouldBe "Attribute names cannot have length more than 255."
    }

    private fun serializeDeserialize(attribute: Attribute): Attribute {
        val serialized = Json.encodeToString(attribute)
        return Json.decodeFromString(serialized)
    }
}