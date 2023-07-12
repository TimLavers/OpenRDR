package io.rippledown.cornerstoneview

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import proxy.waitForEvents
import react.FC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test

class CornerstoneSelectorTest {
    @Test
    fun shouldDefaultToFirstCornerstoneIndex() = runTest {
        val vfc = FC {
            CornerstoneSelector {
                total = 42
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireSelectedCornerstoneOneBasedIndex(1)
            }
        }
    }

    @Test
    fun shouldBeAbleToSelectTheNextCornerstoneIndex() = runTest {
        var selectedZeroBasedIndex = -1
        val vfc = FC {
            CornerstoneSelector {
                total = 42
                onSelect = { index ->
                    selectedZeroBasedIndex = index
                }
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            selectNextCornerstone()
            waitForEvents()
            requireSelectedCornerstoneOneBasedIndex(2)
            selectNextCornerstone()
            waitForEvents()
            requireSelectedCornerstoneOneBasedIndex(3)
            selectNextCornerstone()
            waitForEvents()
            requireSelectedCornerstoneOneBasedIndex(4)
            selectedZeroBasedIndex shouldBe 3
        }
    }


    @Test
    fun shouldBeAbleToSelectThePreviousCornerstoneIndex() = runTest {
        var selectedZeroBasedIndex = -1
        val vfc = FC {
            CornerstoneSelector {
                total = 42
                onSelect = { index ->
                    selectedZeroBasedIndex = index
                }
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            selectNextCornerstone()
            selectNextCornerstone()
            selectNextCornerstone()
            waitForEvents()
            requireSelectedCornerstoneOneBasedIndex(4)
            selectPreviousCornerstone()
            selectPreviousCornerstone()
            selectPreviousCornerstone()
            waitForEvents()
            requireSelectedCornerstoneOneBasedIndex(1)
            selectedZeroBasedIndex shouldBe 0
        }
    }

}



