package io.rippledown.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CasesInfoTest {

    @Test
    fun construction() {
        val idList = mutableListOf(CaseId(123, "Case 1"), CaseId(234, "Case 2"))
        val info = CasesInfo(idList, "whatever/blah")
        assertEquals(info.count, 2)
        assertEquals(info.caseIds[0].id, 123)
        assertEquals(info.caseIds[0].name, "Case 1")
        assertEquals(info.caseIds[1].id, 234)
        assertEquals(info.caseIds[1].name, "Case 2")
        assertEquals(info.caseIds.size, 2)
        assertEquals(info.kbName, "whatever/blah")
    }

    @Test
    fun jsonSerialisation() {
        val idList = mutableListOf(CaseId(123, "Case 1"), CaseId(234, "Case 2"))
        val info = CasesInfo(idList, "blah/blah/blah")
        val sd1 = serializeDeserialize(info)
        assertEquals(sd1, info)
    }

    private fun serializeDeserialize(info: CasesInfo): CasesInfo {
        val serialized = Json.encodeToString(info)
        return Json.decodeFromString(serialized)
    }
}