package io.rippledown.model.caseview

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class CaseViewPropertiesTest {
    val abc = Attribute("ABC",1)
    val tsh = Attribute("TSH", 2)
    private val xyz = Attribute("XYZ", 3)

    @Test
    fun construction() {
        CaseViewProperties(listOf(abc, tsh, xyz)).attributes shouldBe listOf(abc, tsh, xyz)
    }

    @Suppress("ReplaceCallWithBinaryOperator")
    @Test
    fun equality() {
        CaseViewProperties(emptyList()).equals(CaseViewProperties(emptyList())) shouldBe true
        CaseViewProperties(listOf(abc, tsh, xyz)).equals(CaseViewProperties(emptyList())) shouldBe false
        CaseViewProperties(listOf(abc, tsh, xyz)).equals(CaseViewProperties(listOf(abc, xyz))) shouldBe false
        CaseViewProperties(listOf(abc, tsh, xyz)).equals(CaseViewProperties(listOf(abc, tsh, xyz))) shouldBe true
        CaseViewProperties(emptyList()).equals(null) shouldBe false
        CaseViewProperties(emptyList()).equals("Whatever") shouldBe false
    }

    @Test
    fun hash() {
        CaseViewProperties(emptyList()).hashCode() shouldBe CaseViewProperties(emptyList()).hashCode()
        CaseViewProperties(listOf(abc, tsh, xyz)).hashCode() shouldBe CaseViewProperties(listOf(abc, tsh, xyz)).hashCode()
    }

    @Test
    fun serialization() {
        val serialized = Json.encodeToString(CaseViewProperties(emptyList()))
        val deserialized = Json.decodeFromString<CaseViewProperties>(serialized)
        deserialized shouldBe CaseViewProperties(emptyList())
    }
}