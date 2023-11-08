package io.rippledown.cornerstoneview

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestResult
import proxy.waitForEvents
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class CornerstoneSelectorTest {
    @Test
    fun shouldDefaultToFirstCornerstoneIndex(): TestResult {
        val fc = FC {
            CornerstoneSelector {
                total = 42
            }
        }
        return runReactTest(fc) { container ->
            with(container) {
                requireSelectedCornerstoneOneBasedIndex(1)
            }
        }
    }

    @Test
    fun shouldBeAbleToSelectTheNextCornerstoneIndex(): TestResult {
        var selectedZeroBasedIndex = -1
        val fc = FC {
            CornerstoneSelector {
                total = 42
                onSelect = { index ->
                    selectedZeroBasedIndex = index
                }
            }
        }
        return runReactTest(fc) { container ->
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
    fun shouldBeAbleToSelectThePreviousCornerstoneIndex(): TestResult {
        var selectedZeroBasedIndex = -1
        val fc = FC {
            CornerstoneSelector {
                total = 42
                onSelect = { index ->
                    selectedZeroBasedIndex = index
                }
            }
        }

        return runReactTest(fc) { container ->
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



