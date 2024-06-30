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
import io.rippledown.casecontrol.*
import io.rippledown.constants.main.APPLICATION_BAR_ID
import io.rippledown.constants.main.TITLE
import io.rippledown.interpretation.replaceInterpretationBy
import io.rippledown.interpretation.requireInterpretation
import io.rippledown.model.*
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

    @Test
    fun `should show the interpretation of the first case`() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseIds = listOf(caseId1, caseId2)
        val bondiComment = "Go to Bondi"
        val case = createCaseWithInterpretation(caseA, 1, listOf(bondiComment))
//        coEvery { handler.getCase(1) } returns case
//        coEvery { handler.saveCase(any()) } answers { firstArg() }

        with(composeTestRule) {
            setContent {
//                CaseControl(currentCase = case, casesInfo = CasesInfo(caseIds), handler = handler)
            }
            //Given
            requireNumberOfCasesOnCaseList(2)
            requireNamesToBeShowingOnCaseList(caseA, caseB)

            //When
            waitForCaseToBeShowing(caseA)

            //Then
            requireInterpretation(bondiComment)
        }
    }

    @Test
    fun `should show case list for several cases`() = runTest {
// TODO() MOVE THIS TEST
        val caseIds = (1..10).map { i ->
            val caseId = CaseId(id = i.toLong(), name = "case $i")
//            coEvery { handler.getCase(caseId.id!!) } returns createCase(caseId)
            caseId
        }
//        coEvery { handler.saveCase(any()) } answers { firstArg() }

        val caseName1 = "case 1"
        val caseName10 = "case 10"
        with(composeTestRule) {
            setContent {
//                CaseControl(
//                    currentCase = null,
//                    conditionHints = listOf(),
//                    casesInfo = CasesInfo(caseIds),
//                    handler = handler
//                )
            }
            //Given
            waitForCaseToBeShowing(caseName1)

            //When
            selectCaseByName(caseName10)

            //Then
            waitForCaseToBeShowing(caseName10)
        }
    }

    @Test
    fun `should show a case when its case name is clicked`() = runTest {
        val caseNameA = "case A"
        val caseNameB = "case B"
        val caseNameC = "case C"
        val caseId1 = CaseId(id = 1, name = caseNameA)
        val caseId2 = CaseId(id = 2, name = caseNameB)
        val caseId3 = CaseId(id = 3, name = caseNameC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)
        val caseA = createCase(caseId1)
        val caseB = createCase(caseId2)

//        coEvery { handler.getCase(1) } returns caseA
//        coEvery { handler.getCase(2) } returns caseB
//        coEvery { handler.saveCase(any()) } answers { firstArg() }

        with(composeTestRule) {
            setContent {
//                CaseControl(
//                    currentCase = null,
//                    conditionHints = listOf(),
//                    casesInfo = CasesInfo(threeCaseIds),
//                    handler = handler
//                )
            }
            //Given
            requireNumberOfCasesOnCaseList(3)
            requireNamesToBeShowingOnCaseList(caseNameA, caseNameB, caseNameC)

            //When
            selectCaseByName(caseNameB)

            //Then
            waitForCaseToBeShowing(caseNameB)

        }
    }

    @Test
    fun `should list case names`() = runTest {
        val caseA = "case a"
        val caseB = "case b"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val twoCaseIds = listOf(
            caseId1, caseId2
        )
        val case = createCase(caseId1)
//        coEvery { handler.getCase(1) } returns case
//        coEvery { handler.saveCase(any()) } answers { firstArg() }

        with(composeTestRule) {
            setContent {
//                CaseControl(
//                    currentCase = null,
//                    conditionHints = listOf(),
//                    casesInfo = CasesInfo(twoCaseIds),
//                    handler = handler
//                )
            }
            requireNumberOfCasesOnCaseList(2)
            requireNamesToBeShowingOnCaseList(caseA, caseB)
        }
    }

    @Test
    fun `should update the interpretation when a case is selected`() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseIds = listOf(caseId1, caseId2)
        val bondiComment = "Go to Bondi"
        val malabarComment = "Go to Malabar"

        val viewableCaseA = createCaseWithInterpretation(
            name = caseA,
            id = 1,
            conclusionTexts = listOf(bondiComment)
        )
        val viewableCaseB = createCaseWithInterpretation(
            name = caseB,
            id = 2,
            conclusionTexts = listOf(malabarComment)
        )
//        coEvery { handler.getCase(caseId1.id!!) } returns viewableCaseA
//        coEvery { handler.getCase(caseId2.id!!) } returns viewableCaseB
//        coEvery { handler.saveCase(any()) } answers { firstArg() }

        with(composeTestRule) {
            setContent {
/*
                CaseControl(
                    currentCase = null,
                    conditionHints = listOf(),
                    casesInfo = CasesInfo(caseIds),
                    handler = handler
                )
*/
            }
            //Given
            requireNumberOfCasesOnCaseList(2)
            requireNamesToBeShowingOnCaseList(caseA, caseB)
            waitForCaseToBeShowing(caseA)
            requireInterpretation(bondiComment)

            //When
            selectCaseByName(caseB)

            //Then
            waitForCaseToBeShowing(caseB)
            requireInterpretation(malabarComment)
        }
    }

    @Test
    fun `should not show case selector when a rule session is started`() = runTest {
        with(composeTestRule) {
            setContent {
/*
                CaseControl(
                    currentCase = null,
                    conditionHints = listOf(),
                    casesInfo = CasesInfo(listOf(caseId)),
                    handler = handler
                )
*/
            }
            //Given
//            waitForCaseToBeShowing(caseName)

            //Then
            requireCaseSelectorNotToBeDisplayed()
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
