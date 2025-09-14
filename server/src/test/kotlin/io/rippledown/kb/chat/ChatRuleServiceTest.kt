package io.rippledown.kb.chat

import io.mockk.*
import io.rippledown.model.Conclusion
import io.rippledown.model.RDRCase
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ConditionParsingResult
import io.rippledown.model.rule.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ChatRuleServiceTest {
    private lateinit var getOrCreateConclusion: (String) -> Conclusion
    private lateinit var startRuleSession: (RDRCase, RuleTreeChange) -> CornerstoneStatus
    private lateinit var cornerstoneReviewSessionStarted: () -> Boolean
    private lateinit var addCondition: (Condition) -> Unit
    private lateinit var commitRuleSession: () -> Unit
    private lateinit var conditionForExpression: (String, RDRCase) -> ConditionParsingResult
    private lateinit var undoLastRuleOnKB: () -> Unit
    private lateinit var case: RDRCase
    private lateinit var commentToAdd: String
    private lateinit var commentToRemove: String
    private lateinit var replacedComment: String
    private lateinit var replacementComment: String
    private lateinit var conclusion: Conclusion
    private lateinit var replacementConclusion: Conclusion
    private lateinit var condition1: Condition
    private lateinit var condition2: Condition
    private lateinit var conditions: List<Condition>
    private lateinit var service: ChatRuleService

    @BeforeEach
    fun setUp() {
        getOrCreateConclusion = mockk()
        startRuleSession = mockk()
        cornerstoneReviewSessionStarted = mockk()
        addCondition = mockk()
        commitRuleSession = mockk()
        conditionForExpression = mockk()
        undoLastRuleOnKB = mockk()
        case = mockk<RDRCase>()
        commentToAdd = "Test comment to add"
        commentToRemove = "Test comment to remove"
        replacedComment = "Old comment"
        replacementComment = "New comment"
        conclusion = mockk<Conclusion>()
        replacementConclusion = mockk<Conclusion>()
        condition1 = mockk<Condition>()
        condition2 = mockk<Condition>()
        conditions = listOf(condition1, condition2)
        every { startRuleSession(case, any()) } returns CornerstoneStatus()
        every { cornerstoneReviewSessionStarted() } returns false
        every { addCondition(any()) } just Runs
        every { commitRuleSession() } just Runs
        service = ChatRuleService(
            getOrCreateConclusion,
            startRuleSession,
            cornerstoneReviewSessionStarted,
            addCondition,
            conditionForExpression,
            undoLastRuleOnKB,
            commitRuleSession
        )
    }

    @Test
    fun `buildRuleToAddComment should build rule to add comment`() = runTest {
        // Given
        every { getOrCreateConclusion(commentToAdd) } returns conclusion

        // When
        service.buildRuleToAddComment(case, commentToAdd, conditions)

        // Then
        verify { getOrCreateConclusion(commentToAdd) }
        verify { cornerstoneReviewSessionStarted() }
        verify { startRuleSession(case, match { it is ChangeTreeToAddConclusion && it.toBeAdded == conclusion }) }
        verify { addCondition(condition1) }
        verify { addCondition(condition2) }
        verify { commitRuleSession() }
    }

    @Test
    fun `buildRuleToRemoveComment should build rule to remove comment`() = runTest {
        // Given
        every { getOrCreateConclusion(commentToRemove) } returns conclusion
        every { startRuleSession(case, any()) } returns CornerstoneStatus()
        every { addCondition(any()) } just Runs
        every { commitRuleSession() } just Runs

        // When
        service.buildRuleToRemoveComment(case, commentToRemove, conditions)

        // Then
        verify { getOrCreateConclusion(commentToRemove) }
        verify { startRuleSession(case, match { it is ChangeTreeToRemoveConclusion && it.toBeRemoved == conclusion }) }
        verify { addCondition(condition1) }
        verify { commitRuleSession() }
    }

    @Test
    fun `buildRuleToReplaceComment should build rule to replace comment`() = runTest {
        // Given
        every { getOrCreateConclusion(replacedComment) } returns conclusion
        every { getOrCreateConclusion(replacementComment) } returns replacementConclusion

        // When buildRuleToReplaceComment is called
        service.buildRuleToReplaceComment(case, replacedComment, replacementComment, conditions)

        // Then the expected functions are called with the correct parameters
        verify { getOrCreateConclusion(replacedComment) }
        verify { getOrCreateConclusion(replacementComment) }
        verify {
            startRuleSession(
                case,
                match { it is ChangeTreeToReplaceConclusion && it.toBeReplaced == conclusion && it.replacement == replacementConclusion })
        }
        verify { addCondition(condition1) }
        verify { commitRuleSession() }
    }

    @Test
    fun `conditionForExpression should return result from provided function`() = runTest {
        // Given
        val expression = "x > 5"
        val result = mockk<ConditionParsingResult>()
        every { conditionForExpression(expression, case) } returns result

        // When conditionForExpression is called
        val actual = service.conditionForExpression(case, expression)

        // Then the provided function is called and its result is returned
        verify { conditionForExpression(expression, case) }
        assertEquals(result, actual)
    }

    @Test
    fun `undo last rule should undo last rule on KB`() = runTest {
        // Given

        // When conditionForExpression is called
        service.undoLastRule()

        // Then the provided function is called
        verify { undoLastRuleOnKB() }
    }
}