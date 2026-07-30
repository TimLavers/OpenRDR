package io.rippledown.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.utils.randomString
import io.rippledown.utils.serializeDeserialize
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

    @Test
    fun `kind is EXTERNAL by default`() {
        // Given an attribute created without a kind
        val tsh = Attribute(0, "TSH")

        // Then its kind is EXTERNAL
        tsh.kind shouldBe AttributeKind.EXTERNAL
    }

    @Test
    fun `kind can be specified at construction`() {
        // When attributes are created with each KB-assigned kind
        val bmi = Attribute(1, "BMI", AttributeKind.DERIVED)
        val comment = Attribute(2, "DiabetesStatus", AttributeKind.COMMENT)

        // Then the kinds are as given
        bmi.kind shouldBe AttributeKind.DERIVED
        comment.kind shouldBe AttributeKind.COMMENT
    }

    @Test
    fun `kind survives serialization`() {
        // Given attributes of each kind
        val external = Attribute(1, "TSH")
        val derived = Attribute(2, "BMI", AttributeKind.DERIVED)
        val comment = Attribute(3, "DiabetesStatus", AttributeKind.COMMENT)

        // When they are serialized and deserialized
        // Then the kinds are unchanged
        serializeDeserialize(external).kind shouldBe AttributeKind.EXTERNAL
        serializeDeserialize(derived).kind shouldBe AttributeKind.DERIVED
        serializeDeserialize(comment).kind shouldBe AttributeKind.COMMENT
    }

    @Test
    fun `json without a kind deserializes as EXTERNAL`() {
        // Given json produced before the kind field existed
        val legacyJson = """{"id":99,"name":"TSH"}"""

        // When it is deserialized
        val attribute = Json.decodeFromString<Attribute>(legacyJson)

        // Then the attribute is external
        attribute.kind shouldBe AttributeKind.EXTERNAL
        attribute.id shouldBe 99
        attribute.name shouldBe "TSH"
    }

    @Test
    fun `equality ignores kind, as it does name`() {
        // Given two attributes with the same id but different kinds
        val external = Attribute(1, "Stuff")
        val derived = Attribute(1, "Stuff", AttributeKind.DERIVED)

        // Then they are equal, since equality is by id alone
        (external == derived) shouldBe true
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
}