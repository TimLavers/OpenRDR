package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.rippledown.constants.main.*
import io.rippledown.sample.SampleKB
import io.rippledown.sample.SampleKB.TSH
import io.rippledown.sample.SampleKB.TSH_CASES
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CreateKBFromSampleTest {

    class SH() : CreateKBFromSampleHandler {
        var kbName: String? = null
        var sampleKB: SampleKB? = null
        var cancelled = false

        override fun createKB(name: String, sample: SampleKB) {
            kbName = name
            sampleKB = sample
        }

        override fun cancel() {
           cancelled = true
        }
    }
    private lateinit var handler: SH

    @get:Rule
    var composeTestRule = createComposeRule()

    @Before
    fun setup() {
        handler = SH()
    }

    @Test
    fun `initial layout`() {
        with(composeTestRule) {
            setContent {
                CreateKBFromSample(handler)
            }
            waitUntilExactlyOneExists(hasContentDescription(CREATE_KB_NAME_FIELD_DESCRIPTION))

            onNodeWithContentDescription("select ${TSH.title()}")
                .assertIsEnabled()
                .assertIsDisplayed()
                .assertIsSelected()

            onNodeWithContentDescription("select ${TSH_CASES.title()}")
                .assertIsEnabled()
                .assertIsDisplayed()
                .assertIsNotSelected()

            onNodeWithContentDescription(CREATE_KB_NAME_FIELD_DESCRIPTION)
                .assertIsEnabled()
                .assertIsDisplayed()
            waitUntilExactlyOneExists(hasText(""))

            onNodeWithContentDescription(CREATE_KB_OK_BUTTON_DESCRIPTION)
                .assertIsNotEnabled()
                .assertIsDisplayed()
                .assertTextEquals(CREATE)
            onNodeWithText(CANCEL)
                .assertIsEnabled()
                .assertIsDisplayed()
                .assertTextEquals(CANCEL)
        }
    }

    @Test
    fun cancel() {
        with(composeTestRule) {
            setContent {
                CreateKBFromSample(handler)
            }
            handler.cancelled shouldBe false
            onNodeWithText(CANCEL).performClick()

            handler.cancelled shouldBe true
        }
    }

    @Test
    fun ok() {
        with(composeTestRule) {
            setContent {
                CreateKBFromSample(handler)
            }
            val data = "Whatever"
            onNodeWithContentDescription(CREATE_KB_NAME_FIELD_DESCRIPTION).performTextInput(data)
            onNodeWithContentDescription(CREATE_KB_OK_BUTTON_DESCRIPTION).performClick()
            handler.kbName shouldBe data
            handler.sampleKB shouldBe TSH
            handler.cancelled shouldBe false
        }
    }

    @Test
    fun `select sample`() {
        with(composeTestRule) {
            setContent {
                CreateKBFromSample(handler)
            }
            val data = "Whatever"
            onNodeWithContentDescription(CREATE_KB_NAME_FIELD_DESCRIPTION).performTextInput(data)
            onNodeWithContentDescription("select ${TSH_CASES.title()}").performClick()
            onNodeWithContentDescription(CREATE_KB_OK_BUTTON_DESCRIPTION).performClick()

            handler.kbName shouldBe data
            handler.sampleKB shouldBe TSH_CASES
            handler.cancelled shouldBe false
        }
    }

    @Test
    fun `kb name validation`() {
        with(composeTestRule) {
            setContent {
                CreateKBFromSample(handler)
            }
            val okButton = onNodeWithContentDescription(CREATE_KB_OK_BUTTON_DESCRIPTION)
            okButton.assertIsNotEnabled()
            onNodeWithContentDescription(CREATE_KB_NAME_FIELD_DESCRIPTION).performTextInput("A")
            onNodeWithContentDescription(CREATE_KB_NAME_FIELD_DESCRIPTION).performTextClearance()
            okButton.assertIsNotEnabled()
            onNodeWithContentDescription(CREATE_KB_NAME_FIELD_DESCRIPTION).performTextInput("Bats")
            okButton.assertIsEnabled()
        }
    }
}
