package io.rippledown.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.startWith
import io.rippledown.utils.randomString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class KBInfoTest {
    private val idMatcher = Regex("[a-z0-9_]+_[0-9]{1,7}")

    @Test
    fun convertNameToIdTest() { //KBM-3
        idMatcher.matches(convertNameToId("Snarky Puppy")) shouldBe true
        convertNameToId("Snarky Puppy") should startWith("snarkypuppy")
        idMatcher.matches(convertNameToId("Glucose")) shouldBe true
        idMatcher.matches(convertNameToId("Glucose1")) shouldBe true
        idMatcher.matches(convertNameToId("General-Chemistry")) shouldBe true
        idMatcher.matches(convertNameToId("Stuff***123")) shouldBe true
        idMatcher.matches(convertNameToId("Whatever--()33")) shouldBe true
    }

    @Test
    fun compareToTest() {
        KBInfo("1", "ABC") shouldBeLessThan  KBInfo("2", "ABC")
        KBInfo("2", "ABC") shouldBeGreaterThan KBInfo("1", "ABC")
        KBInfo("1", "ABC") shouldBeLessThan  KBInfo("1", "BCD")
        KBInfo("2", "ABC") shouldBeLessThan  KBInfo("1", "BCD")
        KBInfo("1", "ABC") shouldBeLessThan  KBInfo("2", "BCD")
    }

    @Test
    fun construction() {
        val info = KBInfo("Thyroids")
        assertEquals(info.name, "Thyroids")
        idMatcher.matches(info.id) shouldBe true
        info.id.startsWith("thyroids_") shouldBe true
    }

    @Test
    fun jsonSerialisation() {
        val info = KBInfo("Thyroids")
        val sd = serializeDeserialize(info)
        assertEquals(sd, info)
    }

    @Test
    fun jsonSerialisationWithExplicitId() {
        val info = KBInfo("thy", "Thyroids")
        val sd = serializeDeserialize(info)
        assertEquals(sd, info)
    }

    @Test //KBId-2
    fun nameNotBlank() {
        shouldThrow<IllegalArgumentException> {
            KBInfo("id123","")
        }.message shouldBe "KBInfo name cannot be blank."
    }

    @Test //KBId-3
    fun nameMustBeLessThan128CharactersInLength() {
        repeat(127) {
            KBInfo("id123", randomString(it + 1))
        }
        shouldThrow<IllegalArgumentException> {
            KBInfo("id123", randomString(128))
        }.message shouldBe "KBInfo names have maximum length 127."
    }

    @Test //KBId-9
    fun nameCannotContainNewline() {
        shouldThrow<IllegalArgumentException> {
            KBInfo("id123","What\never")
        }.message shouldBe "KBInfo name cannot contain a newline."
    }

    @Test //KBId-4
    fun idNotBlank() {
        shouldThrow<IllegalArgumentException> {
            KBInfo("","Name")
        }.message shouldBe "KBInfo id cannot be blank."
    }

    @Test //KBId-5
    fun idMustBeLessThan128CharactersInLength() {
        repeat(127) {
            KBInfo(randomString(it + 1).lowercase(), "name")
        }
        shouldThrow<IllegalArgumentException> {
            KBInfo(randomString(128), "name")
        }.message shouldBe "KBInfo ids have maximum length 127."
    }

    @Test //KBId-9
    fun validIdFormat() {
        val bad = listOf("!a", "@b", "a%c", "s-d")
        bad.forEach {
            shouldThrow<IllegalArgumentException> {
                KBInfo(it, "Name")
            }.message shouldBe "KBInfo id should consist of lower case letters, numbers, and _ only, but got $it."
        }
        val good = listOf("a")
        good.forEach {
                KBInfo(it,"Name")
        }
    }

    @Test
    fun equalityIsById() {
        KBInfo("1", "Glucose") shouldBe KBInfo("1", "Thyroid")
        KBInfo("1", "Glucose") shouldNotBe KBInfo("123", "Thyroid")
        KBInfo("1", "Glucose") shouldNotBe KBInfo("123", "Glucose")
        KBInfo("123", "Glucose") shouldBe KBInfo("123", "Thyroid")
        KBInfo("123abc", "Glucose") shouldNotBe KBInfo("123ab", "Thyroid")
    }

    @Test
    fun hashCodeIsById() {
        KBInfo("123", "Glucose").hashCode() shouldBe KBInfo("123", "Thyroid").hashCode()
    }

    @Test
    fun toStringTest() {
        KBInfo("123", "Glucose").toString() shouldBe "KBInfo(id='123', name='Glucose')"
    }

    private fun serializeDeserialize(info: KBInfo): KBInfo {
        val serialized = Json.encodeToString(KBInfo.serializer(), info)
        return Json.decodeFromString(serialized)
    }
}