package io.rippledown.cornerstoneview

import io.kotest.matchers.shouldBe
import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CornerstoneSelectorTest {
    @Test
    fun shouldDefaultToFirstCornerstoneIndex() {
        val fc = FC {
            CornerstoneSelector {
                total = 42
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                requireSelectedCornerstoneOneBasedIndex(1)
            }
        }
    }

    @Test
    fun shouldBeAbleToSelectTheNextCornerstoneIndex() {
        var selectedZeroBasedIndex = -1
        val fc = FC {
            CornerstoneSelector {
                total = 42
                onSelect = { index ->
                    selectedZeroBasedIndex = index
                }
            }
        }
        runReactTest(fc) { container ->
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
    }


    @Test
    fun shouldBeAbleToSelectThePreviousCornerstoneIndex() {
        var selectedZeroBasedIndex = -1
        val fc = FC {
            CornerstoneSelector {
                total = 42
                onSelect = { index ->
                    selectedZeroBasedIndex = index
                }
            }
        }

        runReactTest(fc) { container ->
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
}



