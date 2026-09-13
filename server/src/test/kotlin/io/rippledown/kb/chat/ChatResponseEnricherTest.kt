package io.rippledown.kb.chat

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.rippledown.chat.FunctionCallHandler
import io.rippledown.constants.chat.*
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
    fun `enrich attaches a generated tip alongside model suggestions`() = runTest {
        // Given
        every { service.currentRuleSessionConditionTexts() } returns emptySet()
        val action = ActionComment(ADD_COMMENT, comment = "Hello", suggestions = listOf("age is young"))
        val original = ChatResponse("Added")

        // When
        val response = enricher.enrich(action, original, null)

        // Then
        response shouldBe original.copy(
            suggestions = listOf("age is young"),
            tip = ChatResponseEnricher.commentVariableTip("TSH")
        )
    }

    @Test
    fun `filtering an empty list does not consult the rule service`() {
        // Given
        val response = ChatResponse("No suggestions", tip = "A tip")

        // When
        val filtered = enricher.withoutConditionsAlreadyInTheRule(response)

        // Then
        filtered shouldBe response
        verify(exactly = 0) { service.currentRuleSessionConditionTexts() }
    }

    @Test
    fun `filtering preserves suggestions when no rule service is available`() {
        // Given
        val caseLess = ChatResponseEnricher(null, buffer)
        val response = ChatResponse("Choose", suggestions = listOf("age is young"))

        // When
        val filtered = caseLess.withoutConditionsAlreadyInTheRule(response)

        // Then
        filtered shouldBe response
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `filtering preserves suggestions when none match the rule`(hasConditions: Boolean) {
        // Given
        every { service.currentRuleSessionConditionTexts() } returns
                if (hasConditions) setOf("age is old") else emptySet()
        val response = ChatResponse("Choose", suggestions = listOf("age is young"))

        // When
        val filtered = enricher.withoutConditionsAlreadyInTheRule(response)

        // Then
        filtered shouldBe response
    }

    @Test
    fun `filtering recognises editable suggestions and preserves remaining order and response metadata`() {
        // Given
        every { service.currentRuleSessionConditionTexts() } returns setOf("age is young")
        val response = ChatResponse(
            "Choose", tip = "Tip", kbListing = KnowledgeBaseListing(listOf("Lens"), emptyList()),
            suggestions = listOf(
                "tear production is reduced", "age is young${SuggestedConditionsHandler.EDITABLE_SUFFIX}",
                "age is young", "age is old"
            )
        )

        // When
        val filtered = enricher.withoutConditionsAlreadyInTheRule(response)

        // Then
        filtered shouldBe response.copy(suggestions = listOf("tear production is reduced", "age is old"))
    }

    @Test
    fun `filtering can remove every suggestion`() {
        // Given
        every { service.currentRuleSessionConditionTexts() } returns setOf("age is young")
        val response = ChatResponse("Choose", suggestions = listOf("age is young"))

        // When
        val filtered = enricher.withoutConditionsAlreadyInTheRule(response)

        // Then
        filtered shouldBe response.copy(suggestions = emptyList())
    }

    @ParameterizedTest
    @ValueSource(strings = [ADD_COMMENT, REMOVE_COMMENT, REPLACE_COMMENT, ASSIGN_DERIVED_VALUE, REMOVE_DERIVED_VALUE, REPLACE_DERIVED_VALUE])
    fun `each session starting action can fetch suggestions without altering other response fields`(action: String) =
        runTest {
            // Given
            every { service.isRuleSessionActive() } returns true
            val handler = mockk<FunctionCallHandler>()
            coEvery { handler.handle(emptyMap()) } coAnswers {
                buffer.suggestions = listOf("age is young")
                "Delivered"
            }
            val withHandler = ChatResponseEnricher(service, buffer, handler)
            val original =
                ChatResponse("Added", tip = "Tip", kbListing = KnowledgeBaseListing(listOf("Lens"), emptyList()))

            // When
            val response = withHandler.ensureSuggestionsAfterStartingRuleSession(ActionComment(action), original)

            // Then
            response shouldBe original.copy(suggestions = listOf("age is young"))
            buffer.consume() shouldBe null
            coVerify(exactly = 1) { handler.handle(emptyMap()) }
        }

    @ParameterizedTest
    @ValueSource(strings = [USER_ACTION, COMMIT_RULE, REMOVE_REASON])
    fun `other actions do not fetch suggestions`(action: String) = runTest {
        // Given
        val handler = mockk<FunctionCallHandler>()
        val withHandler = ChatResponseEnricher(service, buffer, handler)
        val original = ChatResponse("Response")

        // When
        val response = withHandler.ensureSuggestionsAfterStartingRuleSession(ActionComment(action), original)

        // Then
        response shouldBe original
        coVerify(exactly = 0) { handler.handle(any()) }
        verify(exactly = 0) { service.isRuleSessionActive() }
    }

    @Test
    fun `missing suggestion handler or rule service leaves the response alone`() = runTest {
        // Given
        val handler = mockk<FunctionCallHandler>()
        val caseLess = ChatResponseEnricher(null, buffer, handler)
        val original = ChatResponse("Added")
        val action = ActionComment(ASSIGN_DERIVED_VALUE)

        // When
        val noHandler = enricher.ensureSuggestionsAfterStartingRuleSession(action, original)
        val noService = caseLess.ensureSuggestionsAfterStartingRuleSession(action, original)

        // Then
        noHandler shouldBe original
        noService shouldBe original
        coVerify(exactly = 0) { handler.handle(any()) }
    }

    @Test
    fun `suggestion handler failures propagate to the caller`() = runTest {
        // Given
        every { service.isRuleSessionActive() } returns true
        val handler = mockk<FunctionCallHandler>()
        val failure = IllegalStateException("Cannot retrieve suggestions")
        coEvery { handler.handle(emptyMap()) } throws failure
        val withHandler = ChatResponseEnricher(service, buffer, handler)

        // When
        val thrown = shouldThrow<IllegalStateException> {
            withHandler.ensureSuggestionsAfterStartingRuleSession(ActionComment(ADD_COMMENT), ChatResponse("Added"))
        }

        // Then
        thrown shouldBe failure
    }

    @Test
    fun `a non comment action does not consume the tip`() {
        // Given
        val response = ChatResponse("Done")

        // When
        val unrelated = enricher.commentVariableTipFor(ActionComment(USER_ACTION), response, null)
        val comment = enricher.commentVariableTipFor(ActionComment(ADD_COMMENT, comment = "Hello"), response, null)

        // Then
        unrelated shouldBe null
        comment shouldBe ChatResponseEnricher.commentVariableTip("TSH")
    }

    @Test
    fun `a case with no attributes uses the default tip example`() {
        // Given
        val case = mockk<ViewableCase>()
        every { case.attributes() } returns emptyList()

        // When
        val tip =
            enricher.commentVariableTipFor(ActionComment(ADD_COMMENT, comment = "Hello"), ChatResponse("Added"), case)

        // Then
        tip shouldBe ChatResponseEnricher.commentVariableTip("TSH")
    }

    @Test
    fun `no tip is offered without a rule service`() {
        // Given
        val caseLess = ChatResponseEnricher(null, buffer)

        // When
        val tip =
            caseLess.commentVariableTipFor(ActionComment(ADD_COMMENT, comment = "Hello"), ChatResponse("No KB"), null)

        // Then
        tip shouldBe null
    }

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
        val response =
            withHandler.ensureSuggestionsAfterStartingRuleSession(ActionComment(ASSIGN_DERIVED_VALUE), original)

        // Then
        response shouldBe original
    }

    @Test
    fun `suggestions are not fetched for inactive sessions or when already supplied`() = runTest {
        // Given
        every { service.isRuleSessionActive() } returns false
        val handler = mockk<FunctionCallHandler>()
        val withHandler = ChatResponseEnricher(service, buffer, handler)

        // When
        val inactive = withHandler.ensureSuggestionsAfterStartingRuleSession(
            ActionComment(ASSIGN_DERIVED_VALUE),
            ChatResponse("Rejected")
        )
        every { service.isRuleSessionActive() } returns true
        val supplied = withHandler.ensureSuggestionsAfterStartingRuleSession(
            ActionComment(ASSIGN_DERIVED_VALUE),
            ChatResponse("Added", suggestions = listOf("age is young"))
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
        val withVariable =
            enricher.commentVariableTipFor(ActionComment(ADD_COMMENT, comment = "TSH is {TSH}"), original, null)
        val plain = enricher.commentVariableTipFor(ActionComment(ADD_COMMENT, comment = "Hello"), original, null)

        // Then
        withVariable shouldBe null
        plain shouldBe null
    }

    @Test
    fun `a rejected or incomplete comment does not consume the tip and a valid comment uses a case attribute`() =
        runTest {
            // Given
            val case = mockk<ViewableCase>()
            every { case.attributes() } returns listOf(Attribute(1, "Glucose"), Attribute(2, "TSH"))
            val action = ActionComment(ADD_COMMENT, comment = "Hello")

            // When
            val rejected = enricher.commentVariableTipFor(action, ChatResponse(RULE_SESSION_ALREADY_ACTIVE_ERROR), case)
            val incomplete =
                enricher.commentVariableTipFor(ActionComment(ADD_COMMENT), ChatResponse("Missing comment"), case)
            val valid = enricher.commentVariableTipFor(action, ChatResponse("Added"), case)

            // Then
            rejected shouldBe null
            incomplete shouldBe null
            valid shouldBe ChatResponseEnricher.commentVariableTip("Glucose")
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
        val first = enricher.commentVariableTipFor(action, original, null)
        val second = enricher.commentVariableTipFor(action, original, null)
        enricher.reset()
        val restarted = enricher.commentVariableTipFor(action, original, null)

        // Then
        first shouldBe ChatResponseEnricher.commentVariableTip("TSH")
        second shouldBe null
        restarted shouldBe first
    }
}
