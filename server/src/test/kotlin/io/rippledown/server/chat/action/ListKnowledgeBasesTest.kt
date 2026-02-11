package io.rippledown.server.chat.action

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import io.rippledown.model.KBInfo
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlin.test.BeforeTest
import kotlin.test.Test

class ListKnowledgeBasesTest: ServerActionTestBase() {

    @BeforeTest
    override fun setup() {
        super.setup()
    }

    @Test
    fun `do it`() = runBlocking{
        val kbsList = listOf(KBInfo("lipids"), KBInfo("glucose"), KBInfo("CE"))
        every { actionsInterface.kbList()}.returns(kbsList)
        val received = ListKnowledgeBases().applyAction(actionsInterface)
        received shouldBe "CE\nglucose\nlipids"
        verify { actionsInterface.kbList() }
    }
}
