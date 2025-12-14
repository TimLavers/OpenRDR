package io.rippledown.model

import io.kotest.matchers.shouldBe
import io.rippledown.utils.serializeDeserialize
import kotlin.test.Test

internal class ServerChatResultTest {
    @Test
    fun serialisation() {
        val kbInfo = KBInfo("id123", "Glucose")
        val chatResult = ServerChatResult("The Glucose Knowledge Base has been opened", kbInfo)
        serializeDeserialize(chatResult) shouldBe chatResult
    }

    @Test
    fun `serialisation with null KBInfo`() {
        val chatResult = ServerChatResult("The Glucose Knowledge Base has been opened")
        serializeDeserialize(chatResult) shouldBe chatResult
    }
}