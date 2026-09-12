package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.rippledown.chat.FunctionCallHandler
import io.rippledown.constants.chat.ADD_COMMENT
import io.rippledown.constants.chat.ASSIGN_DERIVED_VALUE
import io.rippledown.constants.chat.USER_ACTION
import io.rippledown.kb.chat.action.ChatAction.Companion.RULE_SESSION_ALREADY_ACTIVE_ERROR
import io.rippledown.model.Attribute
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.model.chat.ChatResponse
import io.rippledown.model.chat.KnowledgeBaseListing
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ChatResponseEnricherTest {
    private val service = mockk<RuleService>()
    private val buffer = SuggestionsBuffer()
    private val enricher = ChatResponseEnricher(service, buffer)

    @Test
    fun `model suggestions are filtered while an existing response tip is retained when no replacement is supplied`() =
        runTest {
            // Given
            every { service.currentRuleSessionConditionTexts() } returns setOf("age is young")
            val original = ChatResponse("Choose", tip = "Existing tip")

            // When
            val filtered = enricher.enrich(
                ActionComment(USER_ACTION, suggestions = listOf("age is young", "age is old")),
                original,
                null
            )
            val unchanged = enricher.enrich(ActionComment(USER_ACTION), original, null)

            // Then
            filtered.suggestions shouldBe listOf("age is old")
            unchanged shouldBe original
        }

    @Test
    fun `starting a session fetches missing suggestions and filters them after the handler runs`() = runTest {
        // Given
        every { service.isRuleSessionActive() } returns true
        every { service.currentRuleSessionConditionTexts() } returns setOf("age is young")
        val handler = mockk<FunctionCallHandler>()
        coEvery { handler.handle(emptyMap()) } coAnswers {
            buffer.suggestions = listOf("age is young", "age is old")
            "Delivered"
        }
        val withHandler = ChatResponseEnricher(service, buffer, handler)

        // When
        val response = withHandler.enrich(ActionComment(ASSIGN_DERIVED_VALUE), ChatResponse("Added"), null)

        // Then
        response.suggestions shouldBe listOf("age is old")
        coVerify(exactly = 1) { handler.handle(emptyMap()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `an empty suggestion result preserves the response`(nullResult: Boolean) = runTest {
        // Given
        every { service.isRuleSessionActive() } returns true
        val handler = mockk<FunctionCallHandler>()
        coEvery { handler.handle(emptyMap()) } coAnswers {
            buffer.suggestions = if (nullResult) null else emptyList()
            "No suggestions"
        }
        val withHandler = ChatResponseEnricher(service, buffer, handler)
        val original = ChatResponse("Added")

        // When
        val response = withHandler.enrich(ActionComment(ASSIGN_DERIVED_VALUE), original, null)

        // Then
        response shouldBe original
    }

    @Test
    fun `suggestions are not fetched for inactive sessions or when already supplied`() = runTest {
        // Given
        every { service.isRuleSessionActive() } returns false
        every { service.currentRuleSessionConditionTexts() } returns emptySet()
        val handler = mockk<FunctionCallHandler>()
        val withHandler = ChatResponseEnricher(service, buffer, handler)

        // When
        val inactive = withHandler.enrich(ActionComment(ASSIGN_DERIVED_VALUE), ChatResponse("Rejected"), null)
        every { service.isRuleSessionActive() } returns true
        val supplied = withHandler.enrich(
            ActionComment(ASSIGN_DERIVED_VALUE, suggestions = listOf("age is young")),
            ChatResponse("Added"),
            null
        )

        // Then
        inactive shouldBe ChatResponse("Rejected")
        supplied.suggestions shouldBe listOf("age is young")
        coVerify(exactly = 0) { handler.handle(any()) }
    }

    @Test
    fun `using a variable resolves the tip without displaying it`() = runTest {
        // Given
        val original = ChatResponse("Added")

        // When
        val withVariable = enricher.enrich(ActionComment(ADD_COMMENT, comment = "TSH is {TSH}"), original, null)
        val plain = enricher.enrich(ActionComment(ADD_COMMENT, comment = "Hello"), original, null)

        // Then
        withVariable.tip shouldBe null
        plain.tip shouldBe null
    }

    @Test
    fun `a rejected or incomplete comment does not consume the tip and a valid comment uses a case attribute`() =
        runTest {
            // Given
            val case = mockk<ViewableCase>()
            every { case.attributes() } returns listOf(Attribute(1, "Glucose"))
            val action = ActionComment(ADD_COMMENT, comment = "Hello")

            // When
            val rejected = enricher.enrich(action, ChatResponse(RULE_SESSION_ALREADY_ACTIVE_ERROR), case)
            val incomplete = enricher.enrich(ActionComment(ADD_COMMENT), ChatResponse("Missing comment"), case)
            val valid = enricher.enrich(action, ChatResponse("Added"), case)

            // Then
            rejected.tip shouldBe null
            incomplete.tip shouldBe null
            valid.tip shouldBe ChatResponseEnricher.commentVariableTip("Glucose")
        }

    @Test
    fun `without a rule service suggestions survive and no rule tip is generated`() = runTest {
        // Given
        val caseLess = ChatResponseEnricher(null, buffer)

        // When
        val response = caseLess.enrich(
            ActionComment(ADD_COMMENT, comment = "Hello", suggestions = listOf("Option")),
            ChatResponse("No KB"),
            null
        )

        // Then
        response shouldBe ChatResponse("No KB", suggestions = listOf("Option"))
    }

    @Test
    fun `buffered suggestions take precedence and exclude conditions already used without losing other content`() =
        runTest {
            // Given
            every { service.currentRuleSessionConditionTexts() } returns setOf("age is young")
            buffer.suggestions = listOf("age is young", "tear production is reduced")
            val listing = KnowledgeBaseListing(listOf("Lens"), emptyList())
            val original = ChatResponse("Choose", kbListing = listing)

            // When
            val response =
                enricher.enrich(ActionComment(USER_ACTION, suggestions = listOf("model suggestion")), original, null)

            // Then
            response shouldBe original.copy(suggestions = listOf("tear production is reduced"))
            buffer.consume() shouldBe null
        }

    @Test
    fun `the variable tip is resolved once per conversation and reset restores it`() = runTest {
        // Given
        val action = ActionComment(ADD_COMMENT, comment = "Hello")
        val original = ChatResponse("Added")

        // When
        val first = enricher.enrich(action, original, null)
        val second = enricher.enrich(action, original, null)
        enricher.reset()
        val restarted = enricher.enrich(action, original, null)

        // Then
        first.tip shouldBe ChatResponseEnricher.commentVariableTip("TSH")
        second.tip shouldBe null
        restarted.tip shouldBe first.tip
    }
}
