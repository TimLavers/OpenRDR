package io.rippledown.server.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import io.rippledown.model.KBInfo
import io.rippledown.model.ServerChatResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test

class OpenKnowledgeBaseTest: ServerActionTestBase() {

    @BeforeTest
    override fun setup() {
        super.setup()
    }

    @Test
    fun `do it`() = runBlocking {
        val lipidsInfo = KBInfo("Lipids")
        every { actionsInterface.openKB(lipidsInfo.name)}.returns(Result.success(lipidsInfo))

        val jsonObject = JsonObject(mapOf("kbName" to JsonPrimitive(lipidsInfo.name)))
        val action = OpenKnowledgeBase(jsonObject)

        val result = action.applyAction(actionsInterface)

        result shouldBe "${lipidsInfo.name} has been opened"
        verify { actionsInterface.openKB(lipidsInfo.name) }
    }

    @Test
    fun `do it with non-existent KB`() = runBlocking {
        val kbName = "NonExistentKB"
        every { actionsInterface.openKB(kbName)}.returns(Result.failure(IllegalArgumentException("Nope")))

        val jsonObject = JsonObject(mapOf("kbName" to JsonPrimitive(kbName)))
        val action = OpenKnowledgeBase(jsonObject)

        val result = action.applyAction(actionsInterface)

        result shouldBe "Nope"
        verify { actionsInterface.openKB(kbName) }
    }
}
