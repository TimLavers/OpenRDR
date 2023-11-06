package io.rippledown.cornerstoneview

import io.kotest.matchers.shouldBe
import io.rippledown.model.createCase
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.TestResult
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CornerstoneViewTest {

    @Test
    //TODO
    fun shouldTruncateLongNames(): TestResult {
        val name = "Long name that should be truncated"
        val case = createCase(name)
        val ccStatus =
            CornerstoneStatus(cornerstoneToReview = case, indexOfCornerstoneToReview = 0, numberOfCornerstones = 42)
        val fc = FC {
            CornerstoneView {
                cornerstoneStatus = ccStatus
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireCornerstoneCaseToBeShowing(name)
            }
        }
    }
}

@Test
fun shouldNotShowCornerstoneViewIfThereIsNoCornerstoneCase(): TestResult {
    val fc = FC {
        CornerstoneView {
            cornerstoneStatus = CornerstoneStatus(cornerstoneToReview = null)
        }
    }
    return runReactTest(fc) { container ->
        with(container) {
            requireCornerstoneCaseNotToBeShowing()
        }
    }
}

@Test
fun shouldHandleCornerstoneSelectionEvents(): TestResult {
    val cc = createCase("Bondi")
    var selectedIndex = -1
    val fc = FC {
        CornerstoneView {
            cornerstoneStatus = CornerstoneStatus(cc, 0, 42)
            selectCornerstone = { index ->
                selectedIndex = index
            }
        }
    }
    return runReactTest(fc) { container ->
        with(container) {
            for (i in 1..30) selectNextCornerstone()
            selectedIndex shouldBe 30
            selectPreviousCornerstone()
            selectedIndex shouldBe 29
        }
    }
}




