package io.rippledown.interpretation

import io.rippledown.model.diff.*
import kotlinx.coroutines.test.TestResult
import mui.material.Button
import proxy.findById
import react.FC
import react.dom.html.ReactHTML.div
import react.dom.test.act
import react.dom.test.runReactTest
import react.useState
import kotlin.test.Test

class DiffViewerUpdateTest {

    @Test
    fun shouldUpdateDiffViewerWhenTheDiffListIsChanged(): TestResult {
        val buttonId = "button_id"
        val diffListA = DiffList(listOf(Addition(), Removal()))
        val diffListB = DiffList(listOf(Unchanged(), Replacement(), Unchanged(), Addition()))

        val fc = FC {
            var currentDiffList by useState(diffListA)

            Button {
                id = buttonId
                onClick = {
                    currentDiffList = diffListB
                }
            }
            div {
                key = diffViewerKey(currentDiffList) //Re-render when the diff list changes

                DiffViewer {
                    diffList = currentDiffList
                }
            }
        }

        return runReactTest(fc) { container ->
            with(container) {
                //Given
                requireNumberOfRows(2)
                requireBuildIconForRow(0) //The first unchanged diff for interpA

                //When switch the diffList
                act { findById(buttonId).click() }

                //Then
                requireNumberOfRows(4)
                requireBuildIconForRow(1) //The first unchanged diff for interpB
            }
        }
    }
}