package io.rippledown.kb.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.chat.AI_UNAVAILABLE_MESSAGE
import io.rippledown.constants.chat.KB_ACTION_DURING_RULE_MESSAGE
import io.rippledown.constants.chat.NAME_THE_NEW_KB
import io.rippledown.constants.chat.demoCaseAddedMessage
import io.rippledown.kb.chat.action.KbManagementAction
import io.rippledown.kb.chat.action.KbManagementOutcome
import io.rippledown.kb.chat.action.done
import io.rippledown.model.KBInfo
import io.rippledown.model.RDRCase
import io.rippledown.model.chat.ChatResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class KnowledgeBaseConversationTest {
    private val service = mockk<KnowledgeBaseService>()
    private val interpreter = mockk<KbCreationReplyInterpreter>()
    private val rules = mockk<RuleService>()
    private val conversation = KnowledgeBaseConversation(service, interpreter, rules)

    @Test
    fun `an ordinary greeting offers a demo case for the open knowledge base only once`() = runTest {
        // Given
        every { rules.isRuleSessionActive() } returns false
        every { service.knowledgeBases() } returns listOf(KBInfo("id", "Lens"))
        every { service.openKnowledgeBase() } returns KBInfo("id", "Lens")
        val case = mockk<RDRCase>()
        every { case.name } returns "Case1"
        coEvery { service.addDemonstrationCase() } returns case
        conversation.greet(null, "Add a demonstration case?")

        // When
        val response = conversation.answer("yes")
        val repeated = conversation.answer("yes")

        // Then
        response shouldBe ChatResponse(demoCaseAddedMessage("Case1"))
        repeated shouldBe null
        conversation.state shouldBe KnowledgeBaseConversation.State.Idle
        coVerify(exactly = 1) { service.addDemonstrationCase() }
    }

    @ParameterizedTest
    @ValueSource(strings = ["no", "open something else"])
    fun `a different reply consumes a confirmation without executing it`(message: String) = runTest {
        // Given
        val action = mockk<KbManagementAction>()
        every { action.changesContext } returns false
        val confirmed = mockk<suspend (KnowledgeBaseService) -> ChatResponse>()
        coEvery { action.doIt(service) } returns KbManagementOutcome.Ask("Delete?", confirmed)
        conversation.execute(action)

        // When
        val response = conversation.answer(message)
        val later = conversation.answer("yes")

        // Then
        response shouldBe null
        later shouldBe null
        coVerify(exactly = 0) { confirmed(any()) }
    }

    @ParameterizedTest
    @EnumSource(KbCreationIntent::class, names = ["DENY", "OTHER_REQUEST"])
    fun `declining or changing subject leaves the naming workflow`(intent: KbCreationIntent) = runTest {
        // Given
        val action = mockk<KbManagementAction>()
        every { action.changesContext } returns false
        coEvery { action.doIt(service) } returns KbManagementOutcome.AskForName("Name?") { mockk() }
        conversation.execute(action)
        coEvery { interpreter.interpret(any(), any(), "cancel") } returns KbCreationReply(intent)

        // When
        val response = conversation.answer("cancel")

        // Then
        response shouldBe if (intent == KbCreationIntent.DENY) {
            ChatResponse(KnowledgeBaseConversation.KB_CREATION_DECLINED)
        } else null
        conversation.state shouldBe KnowledgeBaseConversation.State.Idle
        conversation.answer("yes") shouldBe null
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `unclear replies clarify the current stage and retain the naming action`(naming: Boolean) = runTest {
        // Given
        every { service.knowledgeBases() } returns emptyList()
        every { service.openKnowledgeBase() } returns null
        conversation.greet(null, "Create?")
        if (naming) conversation.answer("yes")
        coEvery { interpreter.interpret(any(), any(), "maybe") } returns KbCreationReply(KbCreationIntent.UNCLEAR)

        // When
        val response = conversation.answer("maybe")

        // Then
        response shouldBe ChatResponse(
            if (naming) KnowledgeBaseConversation.KB_NAME_CLARIFICATION
            else KnowledgeBaseConversation.KB_CREATION_CLARIFICATION
        )
        conversation.state.shouldBeInstanceOf<KnowledgeBaseConversation.State.Creating>().question shouldBe response?.text
    }

    @Test
    fun `interpretation failures preserve the workflow and cancellation propagates`() = runTest {
        // Given
        every { service.knowledgeBases() } returns emptyList()
        every { service.openKnowledgeBase() } returns null
        conversation.greet(null, "Create?")
        coEvery { interpreter.interpret(any(), any(), any()) } throws IllegalArgumentException("bad reply")

        // When
        val invalid = conversation.answer("maybe")
        val pending = conversation.state
        coEvery { interpreter.interpret(any(), any(), any()) } throws IllegalStateException("offline")
        val unavailable = conversation.answer("maybe")
        coEvery { interpreter.interpret(any(), any(), any()) } throws CancellationException("cancelled")

        // Then
        invalid shouldBe ChatResponse(KnowledgeBaseConversation.KB_CREATION_CLARIFICATION)
        unavailable shouldBe ChatResponse(AI_UNAVAILABLE_MESSAGE)
        shouldThrow<CancellationException> { conversation.answer("maybe") }
        conversation.state shouldBe pending
    }

    @Test
    fun `interpreted confirmation enters naming and repeated confirmation stays there`() = runTest {
        // Given
        every { service.knowledgeBases() } returns emptyList()
        every { service.openKnowledgeBase() } returns null
        conversation.greet(null, "Create?")
        coEvery { interpreter.interpret(any(), any(), "oui") } returns KbCreationReply(KbCreationIntent.CONFIRM)

        // When
        val first = conversation.answer("oui")
        val second = conversation.answer("oui")

        // Then
        first shouldBe ChatResponse(NAME_THE_NEW_KB)
        second shouldBe first
        conversation.state.shouldBeInstanceOf<KnowledgeBaseConversation.State.Creating>().stage shouldBe
                KbCreationStage.AWAITING_NAME
    }

    @Test
    fun `context changes are blocked during a rule session but read only actions are allowed`() = runTest {
        // Given
        every { rules.isRuleSessionActive() } returns true
        val change = mockk<KbManagementAction>()
        every { change.changesContext } returns true
        val read = mockk<KbManagementAction>()
        every { read.changesContext } returns false
        coEvery { read.doIt(service) } returns done("List")

        // When
        val blocked = conversation.execute(change)
        val allowed = conversation.execute(read)

        // Then
        blocked shouldBe ChatResponse(KB_ACTION_DURING_RULE_MESSAGE)
        allowed shouldBe ChatResponse("List")
        coVerify(exactly = 0) { change.doIt(any()) }
    }

    @Test
    fun `the first greeting transitions through offer and naming without accepting yes as a name`() = runTest {
        // Given
        every { service.knowledgeBases() } returns emptyList()
        every { service.openKnowledgeBase() } returns null
        conversation.greet(null, "Create a knowledge base?")

        // When
        val first = conversation.answer("yes")
        val second = conversation.answer("yes")

        // Then
        first shouldBe ChatResponse(NAME_THE_NEW_KB)
        second shouldBe first
        conversation.state.shouldBeInstanceOf<KnowledgeBaseConversation.State.Creating>().stage shouldBe
                KbCreationStage.AWAITING_NAME
        coVerify(exactly = 0) { interpreter.interpret(any(), any(), any()) }
        coVerify(exactly = 0) { service.create(any()) }
    }

    @Test
    fun `a confirmation is consumed once and reset invalidates it`() = runTest {
        // Given
        val action = mockk<KbManagementAction>()
        every { action.changesContext } returns false
        val confirmed = mockk<suspend (KnowledgeBaseService) -> ChatResponse>()
        coEvery { confirmed(service) } returns ChatResponse("Deleted")
        coEvery { action.doIt(service) } returns KbManagementOutcome.Ask("Delete?", confirmed)
        conversation.execute(action)

        // When
        val accepted = conversation.answer("yes")
        val again = conversation.answer("yes")
        conversation.execute(action)
        conversation.reset()
        val afterReset = conversation.answer("yes")

        // Then
        accepted shouldBe ChatResponse("Deleted")
        again shouldBe null
        afterReset shouldBe null
        conversation.state shouldBe KnowledgeBaseConversation.State.Idle
        coVerify(exactly = 1) { confirmed(service) }
    }

    @Test
    fun `naming can transition to a separate confirmation without losing that state`() = runTest {
        // Given
        every { rules.isRuleSessionActive() } returns false
        val namingAction = mockk<KbManagementAction>()
        val namedAction = mockk<KbManagementAction>()
        every { namingAction.changesContext } returns false
        every { namedAction.changesContext } returns false
        val confirmed = mockk<suspend (KnowledgeBaseService) -> ChatResponse>()
        coEvery { confirmed(service) } returns ChatResponse("Created")
        coEvery { namingAction.doIt(service) } returns KbManagementOutcome.AskForName("Name?") { namedAction }
        coEvery { namedAction.doIt(service) } returns KbManagementOutcome.Ask("Similar name. Create?", confirmed)
        coEvery { interpreter.interpret(KbCreationStage.AWAITING_NAME, "Name?", "Thyroids2") } returns
                KbCreationReply(KbCreationIntent.CONFIRM_WITH_NAME, "Thyroids2")
        conversation.execute(namingAction)

        // When
        val question = conversation.answer("Thyroids2")
        val result = conversation.answer("yes")

        // Then
        question shouldBe ChatResponse("Similar name. Create?")
        result shouldBe ChatResponse("Created")
        conversation.state shouldBe KnowledgeBaseConversation.State.Idle
    }
}
