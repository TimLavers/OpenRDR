package io.rippledown.main

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.rippledown.appbar.assertKbNameIs
import io.rippledown.casecontrol.waitForCaseToBeShowing
import io.rippledown.casecontrol.waitForNumberOfCases
import io.rippledown.constants.main.APPLICATION_BAR_ID
import io.rippledown.constants.main.TITLE
import io.rippledown.interpretation.replaceInterpretationBy
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.CaseId
import io.rippledown.model.CasesInfo
import io.rippledown.model.KBInfo
import io.rippledown.model.createCaseWithInterpretation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import kotlin.test.Test


class OpenRDRUITest {
    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: Handler
    lateinit var api: Api

    @Before
    fun setUp() {
        api = mockk<Api>(relaxed = true)
        handler = mockk<Handler>(relaxed = true)
        coEvery { handler.api } returns api
        coEvery { handler.isClosing } returns { true }
    }

    @Test
    @Ignore("TODO: Fix this test")
    fun `should debounce the saving of a case when its interpretation changes`() {
        val bondiKBInfo = KBInfo("Bondi")

        runBlocking {
            val caseA = "case A"
            val caseId1 = CaseId(id = 1, name = caseA)
            val caseIds = listOf(caseId1)
            val bondiComment = "Go to Bondi"
            val manlyComment = "Go to Manly"
            val bronteComment = "Go to Bronte"
            val viewableCase = createCaseWithInterpretation(caseA, 1, listOf(bondiComment))
            with(handler.api) {
                coEvery { kbList() } returns listOf(bondiKBInfo)
                coEvery { kbInfo() } returns bondiKBInfo
                coEvery { getCase(1) } returns viewableCase
                coEvery { waitingCasesInfo() } returns CasesInfo(caseIds)
                coEvery { saveVerifiedInterpretation(any()) } answers { firstArg() }
            }

            with(composeTestRule) {
                setContent {
                    OpenRDRUI(handler)
                }

                //Given
                waitForNumberOfCases(1)
                waitForCaseToBeShowing(caseA)
                requireInterpretation(bondiComment)

                //When a second change is made quickly after the first
                replaceInterpretationBy(manlyComment)
                replaceInterpretationBy(bronteComment)
                waitForIdle()

                //Then only the last change is saved
                coVerify(exactly = 1) { api.saveVerifiedInterpretation(any()) }
            }
        }
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

    @Test
    fun `should show the first project if there is one`() = runTest {
        coEvery { handler.api.kbList() } returns listOf(KBInfo("Bondi"), KBInfo("Malabar"))
        with(composeTestRule) {
            setContent {
                OpenRDRUI(handler)
            }
            assertKbNameIs("Bondi")
        }
    }
}

fun main() {

    val caseIds = (1..100).map { i ->
        CaseId(id = i.toLong(), name = "case $i")
    }
    val handler = mockk<Handler>(relaxed = true)
    val api = mockk<Api>(relaxed = true)
    coEvery { handler.api } returns api
    coEvery { handler.isClosing() } returns false
    coEvery { api.waitingCasesInfo() } returns CasesInfo(caseIds)
    coEvery { api.getCase(any()) } returns createCaseWithInterpretation("case A", 1, listOf("Go to Bondi"))

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
