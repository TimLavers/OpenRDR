package io.rippledown.main

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.every
import io.mockk.mockk
import io.rippledown.constants.main.APPLICATION_BAR_ID
import io.rippledown.constants.main.TITLE
import io.rippledown.model.CaseId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test


class OpenRDRUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: Handler

    @Before
    fun setUp() {
        handler = mockk<Handler>(relaxed = true)
        every { handler.isClosing } returns { true }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `should show OpenRDR UI`() = runTest {
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler)
            }
            onNodeWithTag(testTag = APPLICATION_BAR_ID).assertExists()
        }
    }
}

fun main() {

    val caseIds = (1..100).map { i ->
        CaseId(id = i.toLong(), name = "case $i")
    }
    val handler = mockk<Handler>(relaxed = true)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            icon = painterResource("water-wave-icon.png"),
            title = TITLE
        ) {
            OpenRDRUI(handler)
        }
    }
}
/*

    @Test
    fun shouldNotShowEmptyCaseQueueTest(): TestResult {
        val fc = FC {
            OpenRDRUI {
                scope = MainScope()
                api = Api(defaultMock)
            }
        }
        return runReactTest(fc) { container ->
            container.requireNumberOfCasesNotToBeShowing()
        }
    }

    @Test
    fun caseViewShouldBeInitialisedWithTheCasesFromTheServer(): TestResult {
        val config = config {
            val caseId1 = CaseId(1, "case 1")
            val caseId2 = CaseId(2, "case 2")
            val caseId3 = CaseId(3, "case 3")
            returnCasesInfo = CasesInfo(
                listOf(
                    caseId1,
                    caseId2,
                    caseId3
                )
            )
            returnCase = createCase(caseId1)
        }
        val fc = FC {
            OpenRDRUI {
                scope = MainScope()
                api = Api(mock(config))
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                waitForNextPoll()
                findById(NUMBER_OF_CASES_ID).textContent shouldBe "$CASES 3"
            }
        }
    }
*/
