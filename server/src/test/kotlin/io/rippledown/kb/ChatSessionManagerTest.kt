package io.rippledown.kb

import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import io.rippledown.kb.chat.KBReasonTransformer
import io.rippledown.kb.chat.ModelResponder
import io.rippledown.kb.chat.RuleService
import io.rippledown.model.caseview.ViewableCase
import kotlin.test.BeforeTest
import kotlin.test.Test

class ChatSessionManagerTest {
    private lateinit var ruleSessionManager: RuleSessionManager
    private lateinit var chatSessionManager: ChatSessionManager

    @BeforeTest
    fun setup() {
        ruleSessionManager = mockk()
        chatSessionManager = ChatSessionManager(ruleSessionManager)
    }

    @Test
    fun `should create a KBReasonTransformer when createReasonTransformer is called`() {
        // Given
        val viewableCase = mockk<ViewableCase>()
        val ruleService = mockk<RuleService>()
        val modelResponder = mockk<ModelResponder>()

        // When
        val transformer = chatSessionManager.createReasonTransformer(viewableCase, ruleService, modelResponder)

        // Then
        transformer.shouldBeInstanceOf<KBReasonTransformer>()
    }

    @Test
    fun `should create a reason transformer using the provided ruleService`() {
        // Given
        val viewableCase = mockk<ViewableCase>()
        val modelResponder = mockk<ModelResponder>()

        // When - use the ruleSessionManager as the ruleService
        val transformer = chatSessionManager.createReasonTransformer(viewableCase, ruleSessionManager, modelResponder)

        // Then
        transformer.shouldBeInstanceOf<KBReasonTransformer>()
    }
}
