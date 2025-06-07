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

    @Test
    fun truncatedText() {
        Conclusion(0,"Normal results.").truncatedText() shouldBe "Normal results."
        Conclusion(0,"Totally amazing results.").truncatedText() shouldBe "Totally amazing resu..."
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
}