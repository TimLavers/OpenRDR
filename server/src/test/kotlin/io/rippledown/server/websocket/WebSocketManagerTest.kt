package io.rippledown.server.websocket

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.rippledown.constants.chat.CASES_INFO_PREFIX
import io.rippledown.fromJsonString
import io.rippledown.model.CaseId
import io.rippledown.model.CaseType
import io.rippledown.model.CasesInfo
import io.rippledown.toJsonString
import kotlin.test.Test

class WebSocketManagerTest {

    @Test
    fun `CASES_INFO_PREFIX should be the expected value`() {
        //Then
        CASES_INFO_PREFIX shouldBe "CasesInfo:"
    }

    @Test
    fun `should format CasesInfo message with prefix followed by JSON`() {
        //Given
        val casesInfo = CasesInfo(
            caseIds = listOf(CaseId(id = 1, name = "Case1"), CaseId(id = 2, name = "Case2")),
            cornerstoneCaseIds = listOf(CaseId(id = 3, name = "CS1", type = CaseType.Cornerstone)),
            kbName = "TestKB"
        )

        //When
        val message = CASES_INFO_PREFIX + casesInfo.toJsonString<CasesInfo>()

        //Then
        message shouldStartWith CASES_INFO_PREFIX
        val json = message.removePrefix(CASES_INFO_PREFIX)
        val parsed = json.fromJsonString<CasesInfo>()
        parsed shouldBe casesInfo
        parsed.caseIds.size shouldBe 2
        parsed.cornerstoneCaseIds.size shouldBe 1
        parsed.kbName shouldBe "TestKB"
    }

    @Test
    fun `should roundtrip empty CasesInfo through prefix format`() {
        //Given
        val casesInfo = CasesInfo()

        //When
        val message = CASES_INFO_PREFIX + casesInfo.toJsonString<CasesInfo>()
        val json = message.removePrefix(CASES_INFO_PREFIX)
        val parsed = json.fromJsonString<CasesInfo>()

        //Then
        parsed shouldBe casesInfo
        parsed.count shouldBe 0
    }
}
