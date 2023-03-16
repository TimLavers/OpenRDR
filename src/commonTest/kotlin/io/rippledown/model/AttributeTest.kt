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
        val tsh = Attribute("TSH", 0)
        assertEquals(tsh.name, "TSH")
        assertEquals(tsh.id, 0)
    }

    @Test
    fun jsonSerialisation() {
        val tsh = Attribute("TSH", 99)
        val sd = serializeDeserialize(tsh)
        assertEquals(sd.id, tsh.id)
        assertEquals(sd.name, tsh.name)
    }

    @Test
    fun isEquivalent() {
        Attribute("Stuff", 1).isEquivalent(Attribute("Nonsense", 3)) shouldBe false
        Attribute("Stuff", 1).isEquivalent(Attribute("Nonsense", 1)) shouldBe false
        Attribute("Stuff", 1).isEquivalent(Attribute("Stuff", 3)) shouldBe true
        Attribute("Stuff", 1).isEquivalent(Attribute("Stuff", 1)) shouldBe true
        Attribute("Stuff", 1).isEquivalent(Attribute("stuff", 1)) shouldBe false
    }

    @Test
    fun equalsTest() {
        (Attribute("Stuff", 1) == Attribute("Nonsense", 3)) shouldBe false
        (Attribute("Stuff", 1) == Attribute("Nonsense", 1)) shouldBe true
    }

    @Test
    fun hashCodeTest() {
        (Attribute("Stuff", 1).hashCode() == Attribute("Nonsense", 1).hashCode()) shouldBe true
    }

    @Test //Attr-2
    fun nameNotBlank() {
        shouldThrow<IllegalStateException> {
            Attribute("", 53)
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
            Attribute(randomString(it + 1), it)
        }
        shouldThrow<IllegalStateException> {
            Attribute(randomString(256), 256)
        }.message shouldBe "Attribute names cannot have length more than 255."
    }

    private fun serializeDeserialize(attribute: Attribute): Attribute {
        val serialized = Json.encodeToString(attribute)
        return Json.decodeFromString(serialized)
    }
}