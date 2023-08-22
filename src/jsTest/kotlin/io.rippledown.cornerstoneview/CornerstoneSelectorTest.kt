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
        val fc = FC {
            CornerstoneSelector {
                total = 42
            }
        }
        checkContainer(fc) { container ->
            with(container) {
                requireSelectedCornerstoneOneBasedIndex(1)
            }
        }
    }

    @Test
    fun shouldBeAbleToSelectTheNextCornerstoneIndex() = runTest {
        var selectedZeroBasedIndex = -1
        val fc = FC {
            CornerstoneSelector {
                total = 42
                onSelect = { index ->
                    selectedZeroBasedIndex = index
                }
            }
        }
        val container = createRootFor(fc)
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
        val fc = FC {
            CornerstoneSelector {
                total = 42
                onSelect = { index ->
                    selectedZeroBasedIndex = index
                }
            }
        }
        val container = createRootFor(fc)
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



