package io.rippledown.cornerstoneview

import io.rippledown.model.createCase
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.test.runTest
import react.VFC
import react.dom.checkContainer
import kotlin.test.Test

class CornerstoneViewTest {

    @Test
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
        val name = "Long name that should be truncated"
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

}



