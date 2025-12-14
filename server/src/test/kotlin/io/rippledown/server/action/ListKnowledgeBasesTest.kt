package io.rippledown.server.action

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import io.rippledown.model.KBInfo
import kotlinx.serialization.json.JsonObject
import kotlin.test.BeforeTest
import kotlin.test.Test

class ListKnowledgeBasesTest: ActionTestBase() {

    @BeforeTest
    override fun setup() {
        super.setup()
    }

    @Test
    fun `do it`() {
        val kbsList = listOf(KBInfo("lipids"), KBInfo("glucose"), KBInfo("CE"))
        every { actionsInterface.listKnowledgeBases()}.returns(kbsList)
        val received = ListKnowledgeBases(JsonObject(emptyMap())).doIt(actionsInterface)
        received.userMessage shouldBe "CE\nglucose\nlipids"
        verify { actionsInterface.listKnowledgeBases() }
    }
}
