package io.rippledown.kb.chat.action

import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.ChatResponse
import kotlin.test.BeforeTest

open class KbActionTestBase {
    lateinit var kbService: KnowledgeBaseService
    val thyroids = KBInfo("thyroids_1", "Thyroids")
    val glucose = KBInfo("glucose_1", "Glucose")
    val scratch = KBInfo("scratch_1", "Scratch")

    @BeforeTest
    fun setUpKbService() {
        kbService = mockk()
    }

    fun KbManagementOutcome.text(): String = shouldBeInstanceOf<KbManagementOutcome.Done>().response.text

    fun KbManagementOutcome.asAsk(): KbManagementOutcome.Ask = shouldBeInstanceOf<KbManagementOutcome.Ask>()

    suspend fun KbManagementOutcome.Ask.accept(): ChatResponse = thenDo(kbService)
}
