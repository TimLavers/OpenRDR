package io.rippledown.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class KBInfoTest {

    @Test
    fun construction() {
        val info = KBInfo("Thyroids")
        assertEquals(info.name, "Thyroids")
    }

    @Test
    fun jsonSerialisation() {
        val info = KBInfo("Thyroids")
        val sd = serializeDeserialize(info)
        assertEquals(sd, info)
    }

    @Test //KBId-2
    fun nameNotBlank() {
        shouldThrow<IllegalArgumentException> {
            KBInfo("")
        }.message shouldBe "KBInfo name cannot be blank."
    }

    @Test //KBId-3
    fun nameMustBeLessThan128CharactersInLength() {
        repeat(127) {
            KBInfo(randomString(it + 1))
        }
        shouldThrow<IllegalArgumentException> {
            KBInfo(randomString(128))
        }.message shouldBe "KBInfo names have maximum length 127."
    }

    @Test //KBId-9
    fun nameCannotContainNewline() {
        shouldThrow<IllegalArgumentException> {
            KBInfo("What\never")
        }.message shouldBe "KBInfo name cannot contain a newline."
    }

    @Test //KBId-4
    fun idBlank() {
        KBInfo("","Name")
    }

    @Test //KBId-5
    fun idMustBeLessThan128CharactersInLength() {
        repeat(127) {
            KBInfo(randomString(it + 1), "name")
        }
        shouldThrow<IllegalArgumentException> {
            KBInfo(randomString(128), "name")
        }.message shouldBe "KBInfo ids have maximum length 127."
    }

    @Test //KBId-9
    fun idCannotContainNewline() {
        shouldThrow<IllegalArgumentException> {
            KBInfo("123\n456","Name")
        }.message shouldBe "KBInfo id cannot contain a newline."
    }

    @Test
    fun equalityIsById() {
        KBInfo("", "Glucose") shouldBe KBInfo("", "Thyroid")
        KBInfo("", "Glucose") shouldNotBe KBInfo("123", "Thyroid")
        KBInfo("", "Glucose") shouldNotBe KBInfo("123", "Glucose")
        KBInfo("123", "Glucose") shouldBe KBInfo("123", "Thyroid")
        KBInfo("123abc", "Glucose") shouldNotBe KBInfo("123ABC", "Thyroid")
    }

    @Test
    fun hashCodeIsById() {
        KBInfo("", "Glucose").hashCode() shouldBe KBInfo("", "Thyroid").hashCode()
        KBInfo("123", "Glucose").hashCode() shouldBe KBInfo("123", "Thyroid").hashCode()
    }

    @Test
    fun toStringTest() {
        KBInfo("123", "Glucose").toString() shouldBe "KBInfo(id='123', name='Glucose')"
    }

    private fun serializeDeserialize(info: KBInfo): KBInfo {
        val serialized = Json.encodeToString(info)
        return Json.decodeFromString(serialized)
    }
}