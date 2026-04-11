package io.rippledown.interpretation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.interpretation.CONDITION_PREFIX
import io.rippledown.decoration.BACKGROUND_COLOR
import io.rippledown.model.Conclusion
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement
import io.rippledown.model.interpretationview.ViewableInterpretation
import io.rippledown.utils.createViewableInterpretation
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

@ExperimentalFoundationApi
class ReadonlyInterpretationViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: ReadonlyInterpretationViewHandler
    lateinit var modifier: Modifier

    @Before
    fun setUp() {
        handler = mockk(relaxUnitFun = true)
        modifier = Modifier.fillMaxWidth()
    }

    @Test
    fun `should show non-blank interpretation`() = runTest {
        val text = "Go to Bondi now!"
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    createViewableInterpretation(mapOf(text to emptyList())),
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretationForCornerstone(text)
        }
    }

    @Test
    fun `should show a blank interpretation`() = runTest {
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(createViewableInterpretation(), modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone("")
        }
    }

    @Test
    fun `should not show tool tip for a blank interpretation`() = runTest {
        with(composeTestRule) {
            //Given
            setContent {
                ToolTipForNonEmptyInterpretation(
                    commentIndex = -1,
                    conclusionList = emptyList(),
                    interpretation = mockk()
                )
            }
            //When
            //Then
            onNodeWithContentDescription(label = CONDITION_PREFIX, substring = true).assertDoesNotExist()
        }
    }

    @Test
    fun `should show tool tip for a non-blank interpretation and non-empty condition list`() = runTest {
        //Given
        val interpretation = mockk<ViewableInterpretation>()
        val conclusion = Conclusion(
            42,
            "meaning of life"
        )
        val condition1 = "surf's up"
        val condition2 = "it's sunny"
        every { interpretation.conditionsForConclusion(conclusion) } returns listOf(condition1, condition2)

        with(composeTestRule) {
            //When
            setContent {
                ToolTipForNonEmptyInterpretation(
                    commentIndex = 0,
                    conclusionList = listOf(conclusion),
                    interpretation = interpretation
                )
            }
            //Then
            onNodeWithContentDescription(label = "$CONDITION_PREFIX$condition1").assertIsDisplayed()
            onNodeWithContentDescription(label = "$CONDITION_PREFIX$condition2").assertIsDisplayed()
        }
    }

    @Test
    fun `should highlight comment under the pointer`() = runTest {
        //Given
        val bondiComment = "Bondi."
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        lateinit var textLayoutResult: TextLayoutResult
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }

        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(interpretation, modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone(bondiComment)

            //When
            movePointerOverComment(bondiComment, textLayoutResult)
            waitForIdle()

            //Then
            requireCommentToBeHighlighted(bondiComment, textLayoutResult)
        }
    }

    @Test
    fun `should highlight comment under the pointer when showing two comments`() = runTest {
        //Given
        val bondiComment = "Bondi."
        val malabarComment = "Malabar."
        val interpretation =
            createViewableInterpretation(mapOf(bondiComment to emptyList(), malabarComment to emptyList()))
        val conclusionTexts = interpretation.conclusions().map { it.text }
        val unhighlighted = conclusionTexts.unhighlighted().text
        lateinit var textLayoutResult: TextLayoutResult
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(interpretation, modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone(unhighlighted)

            //When
            movePointerOverComment(malabarComment, textLayoutResult)
            waitForIdle()

            //Then
            requireCommentToBeHighlighted(malabarComment, textLayoutResult)
        }
    }

    @Test
    fun `should not highlight a comment if the pointer is not over it`() = runTest {
        //Given
        val bondiComment = "Bondi."
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        val conclusionTexts = interpretation.conclusions().map { it.text }
        val unhighlighted = conclusionTexts.unhighlighted().text
        lateinit var textLayoutResult: TextLayoutResult
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(interpretation, modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone(unhighlighted)
            movePointerOverComment(bondiComment, textLayoutResult)
            waitForIdle()
            requireCommentToBeHighlighted(bondiComment, textLayoutResult)

            //When
            movePointerToTheRightOfTheComment(bondiComment, textLayoutResult)
            waitForIdle()

            //Then
            requireCommentToBeNotHighlighted(textLayoutResult)
        }
    }

    @Test
    fun `should show the conditions for the conclusion under the pointer`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val malabarComment = "Great for a swim!"
        val interpretationText = "$bondiComment $malabarComment"
        val bondiConditions = listOf("Bring your flippers.", "And your sunscreeen.")
        val malabarConditions = listOf("Great for a swim!", "And a picnic.")
        val interpretation = createViewableInterpretation(
            mapOf(
                bondiComment to bondiConditions,
                malabarComment to malabarConditions
            )
        )
        lateinit var textLayoutResult: TextLayoutResult
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(interpretation, modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone(interpretationText)

            //When
            movePointerOverComment(malabarComment, textLayoutResult)

            //Then
            requireConditionsToBeShowing(malabarConditions)
            requireInterpretationForCornerstone(interpretationText)
        }
    }

    @Test
    fun `should show comment but not show any conditions for the conclusion under the pointer if there are none`() =
        runTest {
            //Given
            val bondiComment = "Best surf in the world!"
            val interpretation = createViewableInterpretation(
                mapOf(bondiComment to listOf())
            )
            lateinit var textLayoutResult: TextLayoutResult
            val handler = object : ReadonlyInterpretationViewHandler by handler {
                override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                    textLayoutResult = layoutResult
                }
            }
            with(composeTestRule) {
                setContent {
                    ReadonlyInterpretationView(interpretation, modifier = modifier, handler = handler)
                }
                requireInterpretationForCornerstone(bondiComment)

                //When
                movePointerOverComment(bondiComment, textLayoutResult)

                //Then
                requireNoConditionsToBeShowing()
                requireInterpretationForCornerstone(bondiComment)
            }
        }


    @Test
    fun `should show comment but not show any conditions if the pointer is not over a comment`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val interpretation = createViewableInterpretation(
            mapOf(bondiComment to listOf())
        )
        lateinit var textLayoutResult: TextLayoutResult
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(interpretation, modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone(bondiComment)

            //When
            movePointerToTheRightOfTheComment(bondiComment, textLayoutResult)

            //Then
            requireNoConditionsToBeShowing()
            requireInterpretationForCornerstone(bondiComment)
        }
    }

    @Test
    fun `should not show change interpretation icon`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val interpretation = createViewableInterpretation(
            mapOf(bondiComment to listOf())
        )
        with(composeTestRule) {
            //When
            setContent {
                ReadonlyInterpretationView(interpretation = interpretation, modifier = modifier, handler = handler)
            }
            requireInterpretationForCornerstone(bondiComment)

            //Then
            requireChangeInterpretationIconToBeNotShowing()
        }
    }

    @Test
    fun `should identify the comment index for a given offset`() {
        with(listOf("01234", "56789")) {
            for (i in 0..4) {
                commentIndexForOffset(i) shouldBe 0
            }
            for (i in 5..9) {
                commentIndexForOffset(i) shouldBe 1
            }
            commentIndexForOffset(10) shouldBe -1
            commentIndexForOffset(-1) shouldBe -1
        }
    }

    @Test
    fun `should highlight the first comment`() {
        with(listOf("01234", "56789")) {
            val annotatedString = highlightItem(0)
            requireStyleForCommentInAnnotatedStringToHaveBackground(annotatedString, this[0], BACKGROUND_COLOR)
            requireStyleForCommentInAnnotatedStringToHaveBackground(annotatedString, this[1], Color.Unspecified)
        }
    }

    @Test
    fun `should highlight the second comment`() {
        with(listOf("01234", "56789")) {
            val annotatedString = highlightItem(1)
            requireStyleForCommentInAnnotatedStringToHaveBackground(annotatedString, this[0], Color.Unspecified)
            requireStyleForCommentInAnnotatedStringToHaveBackground(annotatedString, this[1], BACKGROUND_COLOR)
        }
    }

    @Test
    fun `unhighlighted with Addition diff should append addition text with green background`() {
        val comments = listOf("Bondi.")
        val diff = Addition("Beach time!")
        val annotatedString = comments.unhighlighted(diff)
        annotatedString.text shouldBe "Bondi. Beach time!"
        annotatedString.spanStyles.size shouldBe 1
        val span = annotatedString.spanStyles[0]
        span.item.background shouldBe DIFF_ADDITION_COLOR
        span.start shouldBe "Bondi. ".length
        span.end shouldBe "Bondi. Beach time!".length
    }

    @Test
    fun `unhighlighted with Addition diff and empty comments should show only the addition`() {
        val comments = emptyList<String>()
        val diff = Addition("Beach time!")
        val annotatedString = comments.unhighlighted(diff)
        annotatedString.text shouldBe "Beach time!"
        annotatedString.spanStyles.size shouldBe 1
        val span = annotatedString.spanStyles[0]
        span.item.background shouldBe DIFF_ADDITION_COLOR
        span.start shouldBe 0
        span.end shouldBe "Beach time!".length
    }

    @Test
    fun `unhighlighted with Removal diff should style removed comment with red background`() {
        val comments = listOf("Bondi.", "Malabar.")
        val diff = Removal("Bondi.")
        val annotatedString = comments.unhighlighted(diff)
        annotatedString.text shouldBe "Bondi. Malabar."
        annotatedString.spanStyles.size shouldBe 1
        val span = annotatedString.spanStyles[0]
        span.item.background shouldBe DIFF_REMOVAL_COLOR
        span.start shouldBe 0
        span.end shouldBe "Bondi.".length
    }

    @Test
    fun `unhighlighted with Replacement diff should style original red and append replacement green`() {
        val comments = listOf("Bondi.")
        val diff = Replacement("Bondi.", "Maroubra.")
        val annotatedString = comments.unhighlighted(diff)
        annotatedString.text shouldBe "Bondi. Maroubra."
        annotatedString.spanStyles.size shouldBe 2
        val redSpan = annotatedString.spanStyles.first { it.item.background == DIFF_REMOVAL_COLOR }
        redSpan.start shouldBe 0
        redSpan.end shouldBe "Bondi.".length
        val greenSpan = annotatedString.spanStyles.first { it.item.background == DIFF_ADDITION_COLOR }
        greenSpan.start shouldBe "Bondi. ".length
        greenSpan.end shouldBe "Bondi. Maroubra.".length
    }

    @Test
    fun `highlightItem should not apply hover highlight to a Removal diff target`() {
        val comments = listOf("Bondi.")
        val diff = Removal("Bondi.")
        val annotatedString = comments.highlightItem(0, diff)
        annotatedString.spanStyles.size shouldBe 1
        val span = annotatedString.spanStyles[0]
        span.item.background shouldBe DIFF_REMOVAL_COLOR
    }

    @Test
    fun `highlightItem should not apply hover highlight to a Replacement diff target`() {
        val comments = listOf("Bondi.")
        val diff = Replacement("Bondi.", "Maroubra.")
        val annotatedString = comments.highlightItem(0, diff)
        val backgrounds = annotatedString.spanStyles.map { it.item.background }
        backgrounds shouldBe listOf(DIFF_REMOVAL_COLOR, DIFF_ADDITION_COLOR)
    }

    @Test
    fun `should show addition diff appended to existing interpretation`() = runTest {
        val bondiComment = "Go to Bondi."
        val addedComment = "Beach time!"
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    interpretation,
                    diff = Addition(addedComment),
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretationForCornerstone("$bondiComment $addedComment")
        }
    }

    @Test
    fun `should show addition diff as first text when interpretation is blank`() = runTest {
        val addedComment = "Beach time!"
        val interpretation = createViewableInterpretation()
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    interpretation,
                    diff = Addition(addedComment),
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretationForCornerstone(addedComment)
        }
    }

    @Test
    fun `should show removal diff with existing comment still visible`() = runTest {
        val bondiComment = "Go to Bondi."
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    interpretation,
                    diff = Removal(bondiComment),
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretationForCornerstone(bondiComment)
        }
    }

    @Test
    fun `should show replacement diff with original and replacement text`() = runTest {
        val bondiComment = "Go to Bondi."
        val replacementComment = "Go to Maroubra."
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    interpretation,
                    diff = Replacement(bondiComment, replacementComment),
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretationForCornerstone("$bondiComment $replacementComment")
        }
    }

    @Test
    fun `should show rule conditions tooltip when hovering over addition diff text`() = runTest {
        val addedComment = "Beach time!"
        val ruleConditions = listOf("UV is high", "Waves is high")
        val interpretation = createViewableInterpretation()
        lateinit var textLayoutResult: TextLayoutResult
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    interpretation,
                    diff = Addition(addedComment),
                    ruleConditions = ruleConditions,
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretationForCornerstone(addedComment)

            //When
            movePointerOverComment(addedComment, textLayoutResult)

            //Then
            requireConditionsToBeShowing(ruleConditions)
        }
    }

    @Test
    fun `should show rule conditions tooltip when hovering over removal diff text`() = runTest {
        val bondiComment = "Go to Bondi."
        val ruleConditions = listOf("UV is high")
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        lateinit var textLayoutResult: TextLayoutResult
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    interpretation,
                    diff = Removal(bondiComment),
                    ruleConditions = ruleConditions,
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretationForCornerstone(bondiComment)

            //When
            movePointerOverComment(bondiComment, textLayoutResult)

            //Then
            requireConditionsToBeShowing(ruleConditions)
        }
    }

    @Test
    fun `should show rule conditions tooltip when hovering over replacement diff text`() = runTest {
        val bondiComment = "Go to Bondi."
        val replacementComment = "Go to Maroubra."
        val ruleConditions = listOf("UV is high", "Waves is high")
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        lateinit var textLayoutResult: TextLayoutResult
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    interpretation,
                    diff = Replacement(bondiComment, replacementComment),
                    ruleConditions = ruleConditions,
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretationForCornerstone("$bondiComment $replacementComment")

            //When
            movePointerOverComment(replacementComment, textLayoutResult)

            //Then
            requireConditionsToBeShowing(ruleConditions)
        }
    }

    @Test
    fun `should not show rule conditions tooltip when hovering over non-diff text`() = runTest {
        val bondiComment = "Go to Bondi."
        val addedComment = "Beach time!"
        val ruleConditions = listOf("UV is high")
        val interpretation = createViewableInterpretation(mapOf(bondiComment to emptyList()))
        lateinit var textLayoutResult: TextLayoutResult
        val handler = object : ReadonlyInterpretationViewHandler by handler {
            override fun onTextLayoutResult(layoutResult: TextLayoutResult) {
                textLayoutResult = layoutResult
            }
        }
        with(composeTestRule) {
            setContent {
                ReadonlyInterpretationView(
                    interpretation,
                    diff = Addition(addedComment),
                    ruleConditions = ruleConditions,
                    modifier = modifier,
                    handler = handler
                )
            }
            requireInterpretationForCornerstone("$bondiComment $addedComment")

            //When
            movePointerOverComment(bondiComment, textLayoutResult)

            //Then
            requireNoConditionsToBeShowing()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun main() {
    //Given
    val bondiComment = "Best surf in the world!"
    val malabarComment = "Great for a swim!"
    val bondiConditions = listOf("Bring your flippers.", "And your sunscreeen.")
    val malabarConditions = listOf("Great for a swim!", "And a picnic.")
    val interpretation = createViewableInterpretation(
        mapOf(
            bondiComment to bondiConditions,
            malabarComment to malabarConditions
        )
    )
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            ReadonlyInterpretationView(interpretation, modifier = Modifier, handler = mockk())
        }
    }
}