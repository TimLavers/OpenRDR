package io.rippledown.cornerstoneview

import io.kotest.matchers.shouldBe
import io.rippledown.model.createCase
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import react.VFC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test

class CornerstoneViewTest {

    @Test
    //TODO
    fun shouldTruncateLongNames() = runTest {
        val name = "Long name that should be truncated"
        val case = createCase(name)
        val ccStatus =
            CornerstoneStatus(cornerstoneToReview = case, indexOfCornerstoneToReview = 0, numberOfCornerstones = 42)
        val vfc = VFC {
            CornerstoneView {
                cornerstoneStatus = ccStatus
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireCornerstoneCaseToBeShowing(name)
            }
        }
    }

    @Test
    fun shouldNotShowCornerstoneViewIfThereIsNoCornerstoneCase() = runTest {
        val vfc = VFC {
            CornerstoneView {
                cornerstoneStatus = CornerstoneStatus(cornerstoneToReview = null)
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireCornerstoneCaseNotToBeShowing()
            }
        }
    }

    @Test
    fun shouldHandleCornerstoneSelectionEvents() = runTest {
        val cc = createCase("Bondi")
        var selectedIndex = -1
        val vfc = VFC {
            CornerstoneView {
                cornerstoneStatus = CornerstoneStatus(cc, 0, 42)
                selectCornerstone = { index ->
                    selectedIndex = index
                }
            }
        }
        with(createRootFor(vfc)) {
            for (i in 1..30) selectNextCornerstone()
            selectedIndex shouldBe 30
            selectPreviousCornerstone()
            selectedIndex shouldBe 29
        }
    }
}




