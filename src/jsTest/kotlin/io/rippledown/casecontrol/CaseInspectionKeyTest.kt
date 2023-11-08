package io.rippledown.casecontrol

import io.kotest.matchers.shouldBe
import io.rippledown.caseview.requireCaseToBeShowing
import io.rippledown.constants.caseview.CASE_TABLE_ROW_PREFIX
import io.rippledown.model.*
import kotlinx.coroutines.test.TestResult
import mui.material.Button
import proxy.findAllById
import proxy.findById
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.test.act
import react.dom.test.runReactTest
import react.useState
import kotlin.test.Test

class CaseInspectionKeyTest {

    @Test
    fun shouldReRenderCaseViewIfAttributesHaveBeenReordered(): TestResult {
        val bondi = "Bondi"
        val caseId = CaseId(id = 1, name = bondi)
        val a = Attribute(1, "a")
        val b = Attribute(2, "b")
        val attributeWithValueA = AttributeWithValue(a, TestResult("42"))
        val attributeWithValueB = AttributeWithValue(b, TestResult("43"))
        val initialCase = createCase(caseId, listOf(attributeWithValueA, attributeWithValueB))
        val caseWithReorderedAttributes = createCase(caseId, listOf(attributeWithValueB, attributeWithValueA))
        val buttonId = "button_id"

        val fc = FC {
            var currentCase by useState(initialCase)

            Button {
                id = buttonId
                onClick = {
                    currentCase = caseWithReorderedAttributes
                }
            }
            div {
                key = caseInspectionKey(currentCase) //Re-render when the current case changes
                CaseInspectionMemo {
                    case = currentCase
                }
            }
        }

        return runReactTest(fc) { container ->
            with(container) {
                //Given
                requireCaseToBeShowing(bondi)
                val rowsInitial = findAllById(CASE_TABLE_ROW_PREFIX)
                rowsInitial.length shouldBe 2
                rowsInitial[0].children[0].textContent shouldBe a.name
                rowsInitial[1].children[0].textContent shouldBe b.name

                rowsInitial[0].children[1].textContent shouldBe "42"
                rowsInitial[1].children[1].textContent shouldBe "43"


                //When update the case
                act { findById(buttonId).click() }

                //Then
                requireCaseToBeShowing(bondi)
                val reorderedRows = findAllById(CASE_TABLE_ROW_PREFIX)
                reorderedRows.length shouldBe 2
                reorderedRows[0].children[0].textContent shouldBe b.name
                reorderedRows[1].children[0].textContent shouldBe a.name

                reorderedRows[0].children[1].textContent shouldBe "43"
                reorderedRows[1].children[1].textContent shouldBe "42"
            }
        }
    }
}