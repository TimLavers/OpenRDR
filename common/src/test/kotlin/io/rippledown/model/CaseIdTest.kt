package io.rippledown.model

import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

internal class CaseIdTest {

    @Test
    fun construction() {
        // Given a case id created without a list type
        val caseId = CaseId(1234, "Case 1")

        // Then the id and name are as given, and the type defaults to Processed
        caseId.id shouldBe 1234
        caseId.name shouldBe "Case 1"
        caseId.type shouldBe CaseListType.Processed
    }

    @Test
    fun constructionWithType() {
        // Given a case id created with a list type
        val caseId = CaseId(1234, "Case 1", CaseListType.Cornerstone)

        // Then that type is retained
        caseId.type shouldBe CaseListType.Cornerstone
    }

    @Test
    fun constructionWithUserDefinedListType() {
        // Given a case id created with a user-defined list type
        val caseId = CaseId(1234, "Case 1", CaseListType("Good"))

        // Then that type is retained, with its spelling
        caseId.type shouldBe CaseListType("Good")
        caseId.type.name shouldBe "Good"
    }

    @Test
    fun secondaryConstructor() {
        // Given a case id created with just a name
        val caseId = CaseId("Case 1")

        // Then the id is null and the name is as given
        caseId.id shouldBe null
        caseId.name shouldBe "Case 1"
    }

    @Test
    fun jsonSerialisation() {
        // Given a case id with a built-in list type
        val caseId = CaseId(1234, "Case 1", CaseListType.Cornerstone)

        // When it is serialised and deserialised
        val sd = serializeDeserialize(caseId)

        // Then the restored case id is equal to the original
        sd shouldBe caseId
    }

    @Test
    fun jsonSerialisationWithUserDefinedListType() {
        // Given a case id with a mixed-case user-defined list type
        val caseId = CaseId(1234, "Case 1", CaseListType("GoOd"))

        // When it is serialised and deserialised
        val sd = serializeDeserialize(caseId)

        // Then the restored case id is equal and the list name spelling is retained
        sd shouldBe caseId
        sd.type.name shouldBe "GoOd"
    }

    @Test
    fun jsonSerialisationNullId() {
        // Given a case id with a null id
        val caseId = CaseId(null, "Case 1")

        // When it is serialised and deserialised
        val sd = serializeDeserialize(caseId)

        // Then the restored case id is equal to the original
        sd shouldBe caseId
    }

    @Test
    fun `the list type is serialised as the bare list name`() {
        // Given a case id with a built-in list type
        val caseId = CaseId(1234, "Case 1", CaseListType.Cornerstone)

        // When it is serialised
        val serialised = Json.encodeToString(caseId)

        // Then the type appears as the bare list name
        serialised shouldBe """{"id":1234,"name":"Case 1","type":"Cornerstone"}"""
    }

    private fun serializeDeserialize(caseId: CaseId): CaseId {
        val serialized = Json.encodeToString(caseId)
        return Json.decodeFromString(serialized)
    }
}
