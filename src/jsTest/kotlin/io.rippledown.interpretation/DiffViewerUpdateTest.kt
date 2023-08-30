package io.rippledown.interpretation

import io.rippledown.model.diff.*
import kotlinx.coroutines.test.runTest
import mui.base.Button
import proxy.findById
import react.FC
import react.dom.createRootFor
import react.dom.html.ReactHTML.div
import react.dom.test.act
import react.useState
import kotlin.test.Test

class DiffViewerUpdateTest {

    @Test
    fun shouldUpdateInterpretationWhenTheDiffListIsChanged() = runTest {
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
                key = "${currentDiffList.hashCode()}" //Force re-render when the diff list changes

                DiffViewer {
                    diffList = currentDiffList
                }
            }
        }

        with(createRootFor(fc)) {
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