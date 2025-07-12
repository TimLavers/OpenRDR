package io.rippledown.casecontrol

import androidx.compose.material.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import io.rippledown.model.Attribute
import io.rippledown.model.CaseId
import io.rippledown.model.TestResult
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.utils.AttributeWithValue
import io.rippledown.utils.createViewableCase
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test

private const val buttonId = "buttonId"

class CaseInspectionUpdateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should recompose case view if the case changes`() = runTest {
        val bondi = "Bondi"
        val malabar = "Malabar"
        val caseIdBondi = CaseId(id = 1, name = bondi)
        val caseIdMalabar = CaseId(id = 2, name = malabar)
        val initialCase = createViewableCase(caseIdBondi)
        val otherCase = createViewableCase(caseIdMalabar)

        with(composeTestRule) {
            setContent {
                CaseInspectionWithButton(initialCase, otherCase)
            }

            //Given
            waitForCaseToBeShowing(bondi)

            //When update the case
            onNodeWithTag(buttonId).performClick()

            //Then
            waitForCaseToBeShowing(malabar)
        }
    }

    @Test
    fun shouldReRenderCaseViewIfAttributesHaveBeenReordered() = runTest {
        val bondi = "Bondi"
        val caseId = CaseId(id = 1, name = bondi)
        val a = Attribute(1, "a")
        val b = Attribute(2, "b")
        val attributeWithValueA = AttributeWithValue(a, TestResult("42"))
        val attributeWithValueB = AttributeWithValue(b, TestResult("43"))
        val initialCase = createViewableCase(caseId, listOf(attributeWithValueA, attributeWithValueB))
        val caseWithReorderedAttributes = createViewableCase(caseId, listOf(attributeWithValueB, attributeWithValueA))

        with(composeTestRule) {
            setContent {
                CaseInspectionWithButton(initialCase, caseWithReorderedAttributes)
            }

            //Given
            waitForCaseToBeShowing(bondi)
//todo: implement this

//                val rowsInitial = findAllById(CASE_TABLE_ROW_PREFIX)
//                rowsInitial.length shouldBe 2
//                rowsInitial[0].children[0].textContent shouldBe a.name
//                rowsInitial[1].children[0].textContent shouldBe b.name
//
//                rowsInitial[0].children[1].textContent shouldBe "42"
//                rowsInitial[1].children[1].textContent shouldBe "43"


            //When update the case
            onNodeWithTag(buttonId).performClick()

            //Then
            waitForCaseToBeShowing(bondi)
//            val reorderedRows = findAllById(CASE_TABLE_ROW_PREFIX)
//            reorderedRows.length shouldBe 2
//            reorderedRows[0].children[0].textContent shouldBe b.name
//            reorderedRows[1].children[0].textContent shouldBe a.name
//
//            reorderedRows[0].children[1].textContent shouldBe "43"
//            reorderedRows[1].children[1].textContent shouldBe "42"
        }
    }
}

@Composable
fun CaseInspectionWithButton(initialCase: ViewableCase, changedCase: ViewableCase) {

    var currentCase by remember { mutableStateOf(initialCase) }
    val handler = mockk<CaseInspectionHandler>(relaxUnitFun = true)
    every { handler.allComments() } returns setOf("Malabar.", "Bondi.")

    CaseInspection(currentCase, false, handler)

    Button(
        onClick = {
            currentCase = changedCase
        },
        modifier = Modifier.testTag(buttonId)
    ) {}
}