package io.rippledown.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.utils.randomString
import kotlinx.serialization.json.Json
import kotlin.test.Test

// ORD1
internal class AttributeTest {

    @Test //Attr-1
    fun construction() {
        val tsh = Attribute(0, "TSH")
        tsh.name shouldBe "TSH"
        tsh.id shouldBe 0
    }

    @Test
    fun jsonSerialisation() {
        val tsh = Attribute(99, "TSH")
        val sd = serializeDeserialize(tsh)
        sd.id shouldBe tsh.id
        sd.name shouldBe tsh.name
    }

    @Test
    fun isEquivalent() {
        Attribute(1, "Stuff").isEquivalent(Attribute(3, "Nonsense")) shouldBe false
        Attribute(1, "Stuff").isEquivalent(Attribute(1, "Nonsense")) shouldBe false
        Attribute(1, "Stuff").isEquivalent(Attribute(3, "Stuff")) shouldBe true
        Attribute(1, "Stuff").isEquivalent(Attribute(1, "Stuff")) shouldBe true
        Attribute(1, "Stuff").isEquivalent(Attribute(1, "stuff")) shouldBe false
    }

    @Test
    fun equalsTest() {
        (Attribute(1, "Stuff") == Attribute(3, "Nonsense")) shouldBe false
        (Attribute(1, "Stuff") == Attribute(1, "Nonsense")) shouldBe true
    }

    @Test
    fun hashCodeTest() {
        (Attribute(1, "Stuff").hashCode() == Attribute(1, "Nonsense").hashCode()) shouldBe true
    }

    @Test //Attr-2
    fun nameNotBlank() {
        shouldThrow<IllegalStateException> {
            Attribute(53, "")
        }.message shouldBe "Attribute names cannot be blank."
    }

    @Test //Attr-3
    fun nameMustBeLessThan256CharactersInLength() {
        repeat(254) {
            Attribute(it, randomString(it + 1))
        }
        shouldThrow<IllegalStateException> {
            Attribute(256, randomString(256))
        }.message shouldBe "Attribute names cannot have length more than 255."
    }

    private fun serializeDeserialize(attribute: Attribute): Attribute {
        val serialized = Json.encodeToString(attribute)
        return Json.decodeFromString(serialized)
    }
}