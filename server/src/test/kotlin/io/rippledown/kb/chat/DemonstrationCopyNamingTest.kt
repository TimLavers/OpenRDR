package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.rippledown.chat.ConversationService
import io.rippledown.constants.chat.*
import io.rippledown.kb.KbResolution
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.ChatResponse
import io.rippledown.sample.SampleKB.ZOO
import io.rippledown.toJsonString
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DemonstrationCopyNamingTest {
    private val conversation = mockk<ConversationService>()
    private val kbService = mockk<KnowledgeBaseService>()
    private var manager = ChatManager(conversation, null, kbService)
    private val created = KBInfo("zoo_2", "Zoo2")
    private val question = nameForDemonstrationCopyMessage("Zoo Animals")

    @BeforeEach
    fun setUp() {
        coEvery { conversation.response("Open Zoo Animals") } returns
                ActionComment(action = OPEN_KNOWLEDGE_BASE, kbName = "Zoo Animals").toJsonString()
        every { kbService.resolve("Zoo Animals") } returns KbResolution.Demonstration(ZOO)
        every { kbService.resolve("Zoo2") } returns KbResolution.NotFound("Zoo2", emptyList())
        every { kbService.isDemonstrationTitle("Zoo2") } returns false
        every { kbService.nearDuplicateOf("Zoo2") } returns null
        coEvery { kbService.createFromSample("Zoo2", ZOO) } returns created
    }

    @Test
    fun `a naming reply copies the demonstration and clears the pending state`() = runTest {
        // Given
        val prompt = slot<String>()
        coEvery { conversation.response(capture(prompt)) } returns namedReply("Zoo2")
        coEvery { conversation.response("Help") } returns
                ActionComment(action = USER_ACTION, message = "How can I help?").toJsonString()
        val opening = manager.processActionComment(ActionComment(action = OPEN_KNOWLEDGE_BASE, kbName = "Zoo Animals"))

        // When
        val copied = manager.response("Zoo2")

        // Then
        opening shouldBe ChatResponse(question)
        copied shouldBe ChatResponse(kbCopiedFromDemonstrationMessage("Zoo2", "Zoo Animals"))
        prompt.captured shouldContain "AWAITING_NAME"
        prompt.captured shouldContain question
        prompt.captured shouldContain "Zoo2"
        coVerify(exactly = 1) { kbService.createFromSample("Zoo2", ZOO) }
        coVerify(exactly = 0) { kbService.create(any()) }

        // When
        val next = manager.response("Help")

        // Then
        next shouldBe ChatResponse("How can I help?")
        coVerify(exactly = 1) { conversation.response("Help") }
        coVerify(exactly = 1) { kbService.createFromSample(any(), any()) }
    }

    @Test
    fun `denial cancels the copy and a later yes is an ordinary message`() = runTest {
        // Given
        manager.response("Open Zoo Animals")
        coEvery { conversation.response(any()) } returns """{"intent":"DENY"}"""
        coEvery { conversation.response("yes") } returns
                ActionComment(action = USER_ACTION, message = "What would you like to do?").toJsonString()

        // When
        val denied = manager.response("no")
        val later = manager.response("yes")

        // Then
        denied shouldBe ChatResponse(KnowledgeBaseConversation.KB_CREATION_DECLINED)
        later shouldBe ChatResponse("What would you like to do?")
        coVerify(exactly = 0) { kbService.createFromSample(any(), any()) }
        coVerify(exactly = 0) { kbService.create(any()) }
        coVerify(exactly = 1) { conversation.response("yes") }
    }

    @ParameterizedTest
    @ValueSource(strings = ["yes", "oui"])
    fun `agreement without a name repeats the demonstration question and preserves the copy action`(reply: String) =
        runTest {
            // Given
            manager.response("Open Zoo Animals")
            coEvery { conversation.response(any()) } returns """{"intent":"CONFIRM"}"""

            // When
            val repeated = manager.response(reply)

            // Then
            repeated shouldBe ChatResponse(question)
            coVerify(exactly = 0) { kbService.createFromSample(any(), any()) }
            coVerify(exactly = 0) { kbService.create(any()) }
            coVerify(exactly = 0) { conversation.response("yes") }

            // When
            coEvery { conversation.response(any()) } returns namedReply("Zoo2")
            val copied = manager.response("call it Zoo2")

            // Then
            copied shouldBe ChatResponse(kbCopiedFromDemonstrationMessage("Zoo2", "Zoo Animals"))
            coVerify(exactly = 1) { kbService.createFromSample("Zoo2", ZOO) }
            coVerify(exactly = 0) { kbService.create(any()) }
        }

    @Test
    fun `another request cancels naming and opens the requested stored knowledge base`() = runTest {
        // Given
        val thyroids = KBInfo("thyroids_1", "Thyroids")
        manager.response("Open Zoo Animals")
        coEvery { conversation.response(any()) } returns """{"intent":"OTHER_REQUEST"}"""
        coEvery { conversation.response("open Thyroids") } returns
                ActionComment(action = OPEN_KNOWLEDGE_BASE, kbName = "Thyroids").toJsonString()
        every { kbService.resolve("Thyroids") } returns KbResolution.Exact(thyroids)
        coEvery { kbService.open(thyroids) } just Runs
        coEvery { conversation.response("Help") } returns
                ActionComment(action = USER_ACTION, message = "How can I help?").toJsonString()

        // When
        val opened = manager.response("open Thyroids")
        val next = manager.response("Help")

        // Then
        opened shouldBe ChatResponse(kbOpenedMessage("Thyroids"))
        next shouldBe ChatResponse("How can I help?")
        coVerify(exactly = 1) { kbService.open(thyroids) }
        coVerify(exactly = 1) { conversation.response("open Thyroids") }
        coVerify(exactly = 0) { kbService.createFromSample(any(), any()) }
    }

    @Test
    fun `an active rule session refuses opening a demonstration without leaving a naming request`() = runTest {
        // Given
        val ruleService = mockk<RuleService>()
        every { ruleService.isRuleSessionActive() } returns true
        manager = ChatManager(conversation, ruleService, kbService)
        coEvery { conversation.response("Zoo2") } returns
                ActionComment(action = USER_ACTION, message = "No naming request is pending.").toJsonString()

        // When
        val refused = manager.processActionComment(ActionComment(action = OPEN_KNOWLEDGE_BASE, kbName = "Zoo Animals"))
        every { ruleService.isRuleSessionActive() } returns false
        val next = manager.response("Zoo2")

        // Then
        refused shouldBe ChatResponse(KB_ACTION_DURING_RULE_MESSAGE)
        next shouldBe ChatResponse("No naming request is pending.")
        verify(exactly = 0) { kbService.resolve("Zoo Animals") }
        coVerify(exactly = 1) { conversation.response("Zoo2") }
        coVerify(exactly = 0) { kbService.createFromSample(any(), any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["yes", "oui"])
    fun `clarification and another agreement preserve the copy action and latest question`(reply: String) = runTest {
        // Given
        manager.response("Open Zoo Animals")
        coEvery { conversation.response(any()) } returns """{"intent":"UNCLEAR"}"""
        val clarification = manager.response("What should I call it?")
        coEvery { conversation.response(any()) } returns """{"intent":"CONFIRM"}"""

        // When
        val repeated = manager.response(reply)
        coEvery { conversation.response(any()) } returns namedReply("Zoo2")
        val copied = manager.response("Zoo2")

        // Then
        clarification shouldBe ChatResponse(KnowledgeBaseConversation.KB_NAME_CLARIFICATION)
        repeated shouldBe clarification
        copied shouldBe ChatResponse(kbCopiedFromDemonstrationMessage("Zoo2", "Zoo Animals"))
        coVerify(exactly = 1) { kbService.createFromSample("Zoo2", ZOO) }
        coVerify(exactly = 0) { kbService.create(any()) }
    }

    @Test
    fun `a new conversation clears the pending demonstration copy`() = runTest {
        // Given
        manager.response("Open Zoo Animals")
        coEvery { conversation.startConversation() } returns "Hello"
        coEvery { conversation.response("Zoo2") } returns
                ActionComment(action = USER_ACTION, message = "What would you like to do?").toJsonString()

        // When
        manager.startConversation(null)
        val next = manager.response("Zoo2")

        // Then
        next shouldBe ChatResponse("What would you like to do?")
        coVerify(exactly = 1) { conversation.response("Zoo2") }
        coVerify(exactly = 0) { kbService.createFromSample(any(), any()) }
    }

    @Test
    fun `a near duplicate copy name transfers to confirmation before creating the copy`() = runTest {
        // Given
        manager.response("Open Zoo Animals")
        coEvery { conversation.response(any()) } returns namedReply("Zoo2")
        every { kbService.nearDuplicateOf("Zoo2") } returns KBInfo("zoo_old", "Zoo")

        // When
        val confirmation = manager.response("Zoo2")

        // Then
        confirmation shouldBe ChatResponse(confirmKbCreateMessage("Zoo2", "Zoo"))
        coVerify(exactly = 0) { kbService.createFromSample(any(), any()) }

        // When
        val copied = manager.response("yes")

        // Then
        copied shouldBe ChatResponse(kbCopiedFromDemonstrationMessage("Zoo2", "Zoo Animals"))
        coVerify(exactly = 1) { kbService.createFromSample("Zoo2", ZOO) }
        coVerify(exactly = 0) { kbService.create(any()) }
        coVerify(exactly = 0) { conversation.response("yes") }
    }

    private fun namedReply(name: String) = mapOf("intent" to "CONFIRM_WITH_NAME", "kbName" to name).toJsonString()
}
