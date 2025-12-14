package io.rippledown.server.action

import io.kotest.matchers.shouldNotBe
import kotlin.test.BeforeTest
import kotlin.test.Test

class ServerActionTest: ActionTestBase() {
    @BeforeTest
    override fun setup() {
        super.setup()
    }

    @Test
    fun `get a list KBs action`() {
        val jsonString = """{"action":ListKnowledgeBases}"""
        extractAction(jsonString) shouldNotBe null
    }

    @Test
    fun `handle json tag`() {
        val jsonString = """```json
            {
                "action": "ListKnowledgeBases"
            }
        ```"""
        extractAction(jsonString) shouldNotBe null
    }
}