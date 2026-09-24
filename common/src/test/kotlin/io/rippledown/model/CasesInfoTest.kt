package io.rippledown.model

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

internal class CasesInfoTest {

    @Test
    fun construction() {
        // Given cases info built with processed case ids and a kb name
        val idList = mutableListOf(CaseId(123, "Case 1"), CaseId(234, "Case 2"))
        val info = CasesInfo(caseIds = idList, kbName = "whatever/blah")

        // Then the case ids and kb name are as given, and there are no user-defined lists
        info.count shouldBe 2
        info.caseIds[0].id shouldBe 123
        info.caseIds[0].name shouldBe "Case 1"
        info.caseIds[1].id shouldBe 234
        info.caseIds[1].name shouldBe "Case 2"
        info.caseIds.size shouldBe 2
        info.kbName shouldBe "whatever/blah"
        info.userDefinedCaseLists shouldBe emptyList()
    }

    @Test
    fun `count should include processed, cornerstone and user-defined list cases`() {
        // Given cases info with processed, cornerstone and user-defined list cases
        val processed = listOf(CaseId(1, "P1"), CaseId(2, "P2"))
        val cornerstones = listOf(CaseId(3, "C1", CaseListType.Cornerstone))
        val userLists = listOf(
            CaseListInfo("Good", listOf(CaseId(4, "U1", CaseListType("Good")))),
            CaseListInfo(
                "Bad",
                listOf(CaseId(5, "U2", CaseListType("Bad")), CaseId(6, "U3", CaseListType("Bad")))
            )
        )
        val info = CasesInfo(
            caseIds = processed,
            cornerstoneCaseIds = cornerstones,
            userDefinedCaseLists = userLists,
            kbName = "kb"
        )

        // Then the count includes the cases in all three kinds of list
        info.count shouldBe 6
        info.caseIds.size shouldBe 2
        info.cornerstoneCaseIds.size shouldBe 1
        info.userDefinedCaseLists.size shouldBe 2
    }

    @Test
    fun `a case list info holds the list name and the ids of its cases`() {
        // Given a case list info for a user-defined list
        val caseIds = listOf(CaseId(4, "U1", CaseListType("Good")), CaseId(7, "U2", CaseListType("Good")))
        val listInfo = CaseListInfo("Good", caseIds)

        // Then the name and case ids are as given
        listInfo.name shouldBe "Good"
        listInfo.caseIds shouldBe caseIds
    }

    @Test
    fun jsonSerialisation() {
        // Given cases info without user-defined lists
        val idList = mutableListOf(CaseId(123, "Case 1"), CaseId(234, "Case 2"))
        val info = CasesInfo(caseIds = idList, kbName = "blah/blah/blah")

        // When it is serialised and deserialised
        val sd1 = serializeDeserialize(info)

        // Then the restored cases info is equal to the original
        sd1 shouldBe info
    }

    @Test
    fun `json serialisation with user-defined case lists`() {
        // Given cases info with a user-defined list
        val userLists = listOf(CaseListInfo("Good", listOf(CaseId(4, "U1", CaseListType("Good")))))
        val info = CasesInfo(
            caseIds = listOf(CaseId(123, "Case 1")),
            userDefinedCaseLists = userLists,
            kbName = "kb"
        )

        // When it is serialised and deserialised
        val sd = serializeDeserialize(info)

        // Then the restored cases info is equal to the original, with the list name spelling retained
        sd shouldBe info
        sd.userDefinedCaseLists[0].name shouldBe "Good"
        sd.userDefinedCaseLists[0].caseIds[0].type.name shouldBe "Good"
    }

    private fun serializeDeserialize(info: CasesInfo): CasesInfo {
        val serialized = Json.encodeToString(info)
        return Json.decodeFromString(serialized)
    }
}
