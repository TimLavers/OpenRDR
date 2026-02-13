package io.rippledown.model

import io.rippledown.utils.serializeDeserialize
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CaseIdTest {

    @Test
    fun construction() {
        val caseId = CaseId(1234, "Case 1")
        assertEquals(caseId.id, 1234)
        assertEquals(caseId.name, "Case 1")
        assertEquals(caseId.type, CaseType.Processed)
    }

    @Test
    fun constructionWithType() {
        val caseId = CaseId(1234, "Case 1", CaseType.Cornerstone)
        assertEquals(caseId.type, CaseType.Cornerstone)
    }

    @Test
    fun secondaryConstructor() {
        val caseId = CaseId("Case 1")
        assertEquals(caseId.id, null)
        assertEquals(caseId.name, "Case 1")
    }

    @Test
    fun jsonSerialisation() {
        val caseId = CaseId(1234, "Case 1", CaseType.Cornerstone )
        val sd = serializeDeserialize(caseId)
        assertEquals(sd, caseId)
    }

    @Test
    fun jsonSerialisationWithApostropheInCaseName() {
        val caseId = CaseId(1234, "Sherlock's finest", CaseType.Cornerstone )
        val sd = serializeDeserialize(caseId)
        assertEquals(sd, caseId)
    }

    @Test
    fun jsonSerialisationNullId() {
        val caseId = CaseId(null, "Case 1")
        val sd = serializeDeserialize(caseId)
        assertEquals(sd, caseId)
    }
}