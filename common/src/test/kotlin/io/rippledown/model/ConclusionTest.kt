package io.rippledown.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.utils.randomString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

// ORD1
internal class ConclusionTest {

    @Test
    fun construction() {
        val conclusion = Conclusion(0,"Normal results.")
        conclusion.text shouldBe  "Normal results."
        conclusion.id shouldBe 0
    }

    @Test
    fun jsonSerialisation() {
        val conclusion = Conclusion(1,"Normal results.")
        val sd = serializeDeserialize(conclusion)
        sd.id shouldBe conclusion.id
        sd.text shouldBe conclusion.text
        assertEquals(sd, conclusion)
    }

    @Test//Conc-1
    fun testEquality() {
        Conclusion(1, "Blah") shouldBe Conclusion(1, "Blah")
        Conclusion(1, "Blah") shouldBe Conclusion(1, "Whatever")
        Conclusion(1, "Blah") shouldNotBe Conclusion(2, "Whatever")
        Conclusion(1, "Blah") shouldNotBe Conclusion(2, "Blah")
    }

    @Test//Conc-2
    fun testHashCode() {
        Conclusion(1, "Blah").hashCode() shouldBe Conclusion(1, "Whatever").hashCode()
    }

    @Test //Conc-2
    fun nameNotBlank() {
        shouldThrow<IllegalStateException> {
            Conclusion(22,"")
        }.message shouldBe "Conclusions cannot be blank."
    }

    @Test //Conc-3
    fun nameMustBeLessThan2049CharactersInLength() {
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
}