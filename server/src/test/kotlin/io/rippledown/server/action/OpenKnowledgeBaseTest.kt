package io.rippledown.server.action

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import io.rippledown.model.KBInfo
import io.rippledown.model.ServerChatResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test

class OpenKnowledgeBaseTest: ActionTestBase() {

    @BeforeTest
    override fun setup() {
        super.setup()
    }

    @Test
    fun `do it`() {
        val lipidsInfo = KBInfo("Lipids")
        every { actionsInterface.openKB(lipidsInfo.name)}.returns(Result.success(lipidsInfo))

        val jsonObject = JsonObject(mapOf("kbName" to JsonPrimitive(lipidsInfo.name)))
        val action = OpenKnowledgeBase(jsonObject)

        val result = action.doIt(actionsInterface)

        result shouldBe ServerChatResult("${lipidsInfo.name} has been opened", lipidsInfo)
        verify { actionsInterface.openKB(lipidsInfo.name) }
    }

    @Test
    fun `do it with non-existent KB`() {
        val kbName = "NonExistentKB"
        every { actionsInterface.openKB(kbName)}.returns(Result.failure(IllegalArgumentException("Nope")))

        val jsonObject = JsonObject(mapOf("kbName" to JsonPrimitive(kbName)))
        val action = OpenKnowledgeBase(jsonObject)

        val result = action.doIt(actionsInterface)

        result shouldBe ServerChatResult("Nope")
        verify { actionsInterface.openKB(kbName) }
    }
}
