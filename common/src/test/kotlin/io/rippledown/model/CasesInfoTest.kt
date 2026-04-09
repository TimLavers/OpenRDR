package io.rippledown.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CasesInfoTest {

    @Test
    fun construction() {
        val idList = mutableListOf(CaseId(123, "Case 1"), CaseId(234, "Case 2"))
        val info = CasesInfo(caseIds = idList, kbName = "whatever/blah")
        assertEquals(info.count, 2)
        assertEquals(info.caseIds[0].id, 123)
        assertEquals(info.caseIds[0].name, "Case 1")
        assertEquals(info.caseIds[1].id, 234)
        assertEquals(info.caseIds[1].name, "Case 2")
        assertEquals(info.caseIds.size, 2)
        assertEquals(info.kbName, "whatever/blah")
    }

    @Test
    fun `count should include both processed and cornerstone cases`() {
        val processed = listOf(CaseId(1, "P1"), CaseId(2, "P2"))
        val cornerstones = listOf(CaseId(3, "C1", CaseType.Cornerstone))
        val info = CasesInfo(caseIds = processed, cornerstoneCaseIds = cornerstones, kbName = "kb")
        assertEquals(info.count, 3)
        assertEquals(info.caseIds.size, 2)
        assertEquals(info.cornerstoneCaseIds.size, 1)
    }

    @Test
    fun jsonSerialisation() {
        val idList = mutableListOf(CaseId(123, "Case 1"), CaseId(234, "Case 2"))
        val info = CasesInfo(caseIds = idList, kbName = "blah/blah/blah")
        val sd1 = serializeDeserialize(info)
        assertEquals(sd1, info)
    }

    private fun serializeDeserialize(info: CasesInfo): CasesInfo {
        val serialized = Json.encodeToString(info)
        return Json.decodeFromString(serialized)
    }
}