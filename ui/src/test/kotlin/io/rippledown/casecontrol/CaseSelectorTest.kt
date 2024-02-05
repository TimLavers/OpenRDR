package io.rippledown.casecontrol

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.kotest.matchers.shouldBe
import io.rippledown.constants.main.TITLE
import io.rippledown.main.Handler
import io.rippledown.main.handlerImpl
import io.rippledown.model.CaseId
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

class CaseSelectorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should list case names `() = runTest {
        val caseA = "case a"
        val caseB = "case b"
        val twoCaseIds = listOf(
            CaseId(id = 1, name = caseA), CaseId(id = 2, name = caseB)
        )
        with(composeTestRule) {
            setContent {
                CaseSelector(object : Handler by handlerImpl, CaseSelectorHandler {
                    override var caseIds= twoCaseIds
                    override var selectCase: (_: Long) -> Unit = {}

                })
            }
            requireNamesToBeShowingOnCaseList(caseA, caseB)
        }
    }

    @Test
    fun `should call selectCase when case is selected by id`() = runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseId3 = CaseId(id = 3, name = caseC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)

        var selectedCaseId: Long = 0

        with(composeTestRule) {
            setContent {
                CaseSelector(object : Handler by handlerImpl, CaseSelectorHandler {
                    override var caseIds= threeCaseIds
                    override var selectCase: (id: Long) -> Unit = {
                        selectedCaseId = it
                    }
                })
            }
            selectedCaseId shouldBe 0
            selectCaseByNameUsingContentDescription(caseId2.name)
            selectedCaseId shouldBe caseId2.id
        }
    }

    @Test
    fun `should set the initial list selection`()= runTest {
        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseId3 = CaseId(id = 3, name = caseC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)
        var selectedCaseName: String? = null

        with(composeTestRule) {
            setContent {
                CaseSelector(object : Handler by handlerImpl, CaseSelectorHandler {
                    override var caseIds = threeCaseIds
                    override var selectCase: (id: Long) -> Unit = {}
                })
            }
//            requireNameOnCaseListToBeSelected(caseB)
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        icon = painterResource("water-wave-icon.png"),
        title = TITLE
    ) {

        val caseA = "case A"
        val caseB = "case B"
        val caseC = "case C"
        val caseId1 = CaseId(id = 1, name = caseA)
        val caseId2 = CaseId(id = 2, name = caseB)
        val caseId3 = CaseId(id = 3, name = caseC)
        val threeCaseIds = listOf(caseId1, caseId2, caseId3)
        CaseSelector(object : Handler by handlerImpl, CaseSelectorHandler {
            override var caseIds = threeCaseIds
            override var selectCase: (id: Long) -> Unit = {}
        })
    }
}
