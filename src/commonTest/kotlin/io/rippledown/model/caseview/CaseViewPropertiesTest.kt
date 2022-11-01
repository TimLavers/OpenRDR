package io.rippledown.model.caseview

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class CaseViewPropertiesTest {
    val abc = Attribute("ABC")
    val tsh = Attribute("TSH")
    val xyz = Attribute("XYZ")

    @Test
    fun construction() {
        val attributeSet = setOf(xyz,abc, tsh)
        CaseViewProperties(emptyMap()).orderAttributes(attributeSet) shouldBe listOf(abc, tsh, xyz)
    }

    @Test
    fun equality() {
        @Suppress("ReplaceCallWithBinaryOperator")
        CaseViewProperties(emptyMap()).equals(CaseViewProperties(emptyMap())) shouldBe true
        CaseViewProperties(emptyMap()).equals(null) shouldBe false
        CaseViewProperties(emptyMap()).equals("Whatever") shouldBe false
    }

    @Test
    fun hash() {
        CaseViewProperties(emptyMap()).hashCode() shouldBe CaseViewProperties(emptyMap()).hashCode()
    }

    @Test
    fun serialization() {
        val format = Json { allowStructuredMapKeys = true }
        val serialized = format.encodeToString(CaseViewProperties(emptyMap()))
        val deserialized = format.decodeFromString<CaseViewProperties>(serialized)
        deserialized shouldBe CaseViewProperties(emptyMap())
    }
}