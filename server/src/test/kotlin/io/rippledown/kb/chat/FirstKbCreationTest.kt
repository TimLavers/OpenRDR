package io.rippledown.kb.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.rippledown.chat.ConversationService
import io.rippledown.constants.chat.*
import io.rippledown.kb.KbResolution
import io.rippledown.model.KBInfo
import io.rippledown.model.chat.ChatResponse
import io.rippledown.toJsonString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class FirstKbCreationTest {
    private val conversation = mockk<ConversationService>()
    private val kbService = mockk<KnowledgeBaseService>()
    private val manager = ChatManager(conversation, null, kbService)

    @BeforeEach
    fun setUp() {
        coEvery { conversation.startConversation() } returns ""
        every { kbService.knowledgeBases() } returns emptyList()
        every { kbService.openKnowledgeBase() } returns null
        every { kbService.resolve(any()) } answers { KbResolution.NotFound(firstArg(), emptyList()) }
        every { kbService.nearDuplicateOf(any()) } returns null
        coEvery { kbService.create(any()) } answers { KBInfo("new", firstArg()) }
    }

    @Test
    fun `French confirmation is interpreted with the question and then the server asks for a name`() = runTest {
        // Given
        val prompt = slot<String>()
        coEvery { conversation.response(capture(prompt)) } returns """{"intent":"CONFIRM"}"""
        manager.startConversation(null, noKbGreeting(emptyList()))

        // When
        val response = manager.response("oui")

        // Then
        response shouldBe ChatResponse(NAME_THE_NEW_KB)
        prompt.captured shouldContain noKbGreeting(emptyList())
        prompt.captured shouldContain "oui"
        coVerify(exactly = 1) { conversation.response(any()) }
        coVerify(exactly = 0) { kbService.create(any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["oui", "sí", "go ahead", "yes"])
    fun `every agreement is sent to the model`(message: String) = runTest {
        // Given
        coEvery { conversation.response(any()) } returns """{"intent":"CONFIRM"}"""
        manager.startConversation(null, noKbGreeting(emptyList()))

        // When
        val response = manager.response(message)

        // Then
        response shouldBe ChatResponse(NAME_THE_NEW_KB)
        coVerify(exactly = 1) { conversation.response(match { it.contains(message) }) }
        coVerify(exactly = 0) { kbService.create(any()) }
    }

    @Test
    fun `agreement with a name creates immediately and leaves the workflow`() = runTest {
        // Given
        coEvery { conversation.response(any()) } returns namedReply("Thyroïde")
        coEvery { conversation.response("Help") } returns
                ActionComment(action = USER_ACTION, message = "How can I help?").toJsonString()
        manager.startConversation(null, noKbGreeting(emptyList()))

        // When
        val created = manager.response("Oui, appelle-la Thyroïde")
        val next = manager.response("Help")

        // Then
        created shouldBe ChatResponse(kbCreatedMessage("Thyroïde"))
        next shouldBe ChatResponse("How can I help?")
        coVerify(exactly = 1) { kbService.create("Thyroïde") }
        coVerify(exactly = 1) { conversation.response("Help") }
    }

    @ParameterizedTest
    @ValueSource(strings = ["Coogee Beach", "Call it Coogee Beach"])
    fun `a naming reply is interpreted in the naming state`(message: String) = runTest {
        // Given
        val prompts = mutableListOf<String>()
        coEvery { conversation.response(capture(prompts)) } returnsMany listOf(
            """{"intent":"CONFIRM"}""", namedReply("Coogee Beach")
        )
        manager.startConversation(null, noKbGreeting(emptyList()))
        manager.response("oui")

        // When
        val response = manager.response(message)

        // Then
        response shouldBe ChatResponse(kbCreatedMessage("Coogee Beach"))
        prompts.last() shouldContain "AWAITING_NAME"
        prompts.last() shouldContain NAME_THE_NEW_KB
        prompts.last() shouldContain message
        coVerify(exactly = 1) { kbService.create("Coogee Beach") }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `denial cancels at either stage and a later yes is not stale consent`(awaitingName: Boolean) = runTest {
        // Given
        coEvery { conversation.response(any()) } returns """{"intent":"CONFIRM"}"""
        manager.startConversation(null, noKbGreeting(emptyList()))
        if (awaitingName) manager.response("oui")
        coEvery { conversation.response(any()) } returns """{"intent":"DENY"}"""
        coEvery { conversation.response("yes") } returns
                ActionComment(action = USER_ACTION, message = "What would you like to do?").toJsonString()

        // When
        val denied = manager.response("Actually, no")
        val later = manager.response("yes")

        // Then
        denied shouldBe ChatResponse(ChatManager.KB_CREATION_DECLINED)
        later shouldBe ChatResponse("What would you like to do?")
        coVerify(exactly = 0) { kbService.create(any()) }
    }

    @Test
    fun `another agreement during naming asks again instead of creating a KB named yes`() = runTest {
        // Given
        coEvery { conversation.response(any()) } returns """{"intent":"CONFIRM"}"""
        manager.startConversation(null, noKbGreeting(emptyList()))
        manager.response("oui")

        // When
        val response = manager.response("yes")

        // Then
        response shouldBe ChatResponse(NAME_THE_NEW_KB)
        coVerify(exactly = 0) { kbService.create(any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `unclear replies retain the pending stage`(awaitingName: Boolean) = runTest {
        // Given
        coEvery { conversation.response(any()) } returns """{"intent":"CONFIRM"}"""
        manager.startConversation(null, noKbGreeting(emptyList()))
        if (awaitingName) manager.response("oui")
        coEvery { conversation.response(any()) } returnsMany listOf(
            """{"intent":"UNCLEAR"}""", """{"intent":"CONFIRM"}"""
        )

        // When
        val unclear = manager.response("Maybe, what is a KB?")
        val retry = manager.response("oui")

        // Then
        unclear shouldBe ChatResponse(
            if (awaitingName) ChatManager.KB_NAME_CLARIFICATION else ChatManager.KB_CREATION_CLARIFICATION
        )
        retry shouldBe ChatResponse(NAME_THE_NEW_KB)
        coVerify(exactly = 0) { kbService.create(any()) }
    }

    @Test
    fun `a reply after clarification includes the question most recently shown to the user`() = runTest {
        // Given
        val prompts = mutableListOf<String>()
        coEvery { conversation.response(capture(prompts)) } returnsMany listOf(
            """{"intent":"UNCLEAR"}""", """{"intent":"CONFIRM"}"""
        )
        manager.startConversation(null, noKbGreeting(emptyList()))
        val clarification = manager.response("What is a KB?")

        // When
        manager.response("oui")

        // Then
        prompts.last() shouldContain clarification.text
        prompts.last() shouldContain "OFFER_CREATION"
    }

    @Test
    fun `starting a new conversation resets the naming stage`() = runTest {
        // Given
        val prompts = mutableListOf<String>()
        coEvery { conversation.response(capture(prompts)) } returns """{"intent":"CONFIRM"}"""
        manager.startConversation(null, noKbGreeting(emptyList()))
        manager.response("oui")

        // When
        manager.startConversation(null, noKbGreeting(emptyList()))
        manager.response("oui")

        // Then
        prompts.last() shouldContain "OFFER_CREATION"
        prompts.last() shouldContain noKbGreeting(emptyList())
        coVerify(exactly = 0) { kbService.create(any()) }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "", "yes", "{}", "{broken", "{\"intent\":\"UNKNOWN\"}",
            "{\"intent\":\"CONFIRM_WITH_NAME\"}",
            "{\"intent\":\"CONFIRM_WITH_NAME\",\"kbName\":\"   \"}",
            "{\"intent\":\"DENY\",\"kbName\":\"Unexpected\"}",
            "{\"intent\":\"CONFIRM\"}{\"intent\":\"DENY\"}",
            "{\"action\":\"CreateKnowledgeBase\",\"kbName\":\"Unapproved\"}",
            "{\"intent\":\"CONFIRM\",\"action\":\"CreateKnowledgeBase\"}"
        ]
    )
    fun `invalid model output cannot execute an action or lose the pending question`(modelOutput: String) = runTest {
        // Given
        coEvery { conversation.response(any()) } returnsMany listOf(modelOutput, """{"intent":"CONFIRM"}""")
        manager.startConversation(null, noKbGreeting(emptyList()))

        // When
        val invalid = manager.response("some reply")
        val retry = manager.response("oui")

        // Then
        invalid shouldBe ChatResponse(ChatManager.KB_CREATION_CLARIFICATION)
        retry shouldBe ChatResponse(NAME_THE_NEW_KB)
        coVerify(exactly = 0) { kbService.create(any()) }
        coVerify(exactly = 0) { kbService.delete(any()) }
    }

    @Test
    fun `model failure during naming preserves the name request for a retry`() = runTest {
        // Given
        coEvery { conversation.response(any()) } returns """{"intent":"CONFIRM"}"""
        manager.startConversation(null, noKbGreeting(emptyList()))
        manager.response("oui")
        coEvery { conversation.response(any()) } throws IllegalStateException("Model unavailable")

        // When
        val failed = manager.response("Call it Thyroid")
        coEvery { conversation.response(any()) } returns namedReply("Thyroid")
        val retry = manager.response("Call it Thyroid")

        // Then
        failed shouldBe ChatResponse(AI_UNAVAILABLE_MESSAGE)
        retry shouldBe ChatResponse(kbCreatedMessage("Thyroid"))
        coVerify(exactly = 1) { kbService.create("Thyroid") }
    }

    @Test
    fun `coroutine cancellation is propagated and does not consume the offer`() = runTest {
        // Given
        coEvery { conversation.response(any()) } throws CancellationException("Cancelled")
        manager.startConversation(null, noKbGreeting(emptyList()))

        // When
        shouldThrow<CancellationException> { manager.response("oui") }
        coEvery { conversation.response(any()) } returns """{"intent":"CONFIRM"}"""
        val retry = manager.response("oui")

        // Then
        retry shouldBe ChatResponse(NAME_THE_NEW_KB)
        coVerify(exactly = 0) { kbService.create(any()) }
    }

    @Test
    fun `an explicit different request is handled by the ordinary conversation`() = runTest {
        // Given
        coEvery { conversation.response(any()) } returns """{"intent":"OTHER_REQUEST"}"""
        coEvery { conversation.response("List KBs") } returns ActionComment(action = LIST_KNOWLEDGE_BASES).toJsonString()
        manager.startConversation(null, noKbGreeting(emptyList()))

        // When
        val response = manager.response("List KBs")

        // Then
        response shouldBe ChatResponse(NO_KNOWLEDGE_BASES)
        coVerify(exactly = 1) { conversation.response("List KBs") }
        coVerify(exactly = 0) { kbService.create(any()) }
    }

    @Test
    fun `server validation still refuses an existing name`() = runTest {
        // Given a KB created elsewhere after the initial greeting
        coEvery { conversation.response(any()) } returns namedReply("Thyroid")
        manager.startConversation(null, noKbGreeting(emptyList()))
        every { kbService.resolve("Thyroid") } returns KbResolution.Exact(KBInfo("existing", "Thyroid"))

        // When
        val response = manager.response("Create Thyroid")

        // Then
        response shouldBe ChatResponse(kbAlreadyExistsMessage("Thyroid"))
        coVerify(exactly = 0) { kbService.create(any()) }
    }

    @Test
    fun `a near duplicate still requires the existing server confirmation`() = runTest {
        // Given a similar KB created elsewhere after the greeting
        coEvery { conversation.response(any()) } returns namedReply("Thyroid")
        manager.startConversation(null, noKbGreeting(emptyList()))
        every { kbService.nearDuplicateOf("Thyroid") } returns KBInfo("existing", "Thyroids")

        // When
        val response = manager.response("Create Thyroid")

        // Then
        response shouldBe ChatResponse(confirmKbCreateMessage("Thyroid", "Thyroids"))
        coVerify(exactly = 0) { kbService.create(any()) }

        // When
        val confirmed = manager.response("yes")

        // Then
        confirmed shouldBe ChatResponse(kbCreatedMessage("Thyroid"))
        coVerify(exactly = 1) { kbService.create("Thyroid") }
        coVerify(exactly = 1) { conversation.response(any()) }
    }

    private fun namedReply(name: String) = mapOf("intent" to "CONFIRM_WITH_NAME", "kbName" to name).toJsonString()
}
