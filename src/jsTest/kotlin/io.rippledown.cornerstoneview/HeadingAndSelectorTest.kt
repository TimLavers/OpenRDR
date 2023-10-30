package io.rippledown.cornerstoneview

import io.kotest.matchers.shouldBe
import react.FC
import react.dom.test.runReactTest
import kotlin.test.Test

class HeadingAndSelectorTest {
    @Test
    fun shouldShowTheSpecifiedCornerstoneName() {
        val caseName = "Bondi"
        val fc = FC {
            HeadingAndSelector {
                name = caseName
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                requireCornerstoneCaseToBeShowing(caseName)
            }
        }
    }

    @Test
    fun shouldSelectTheNextCornerstoneIndex() {
        val caseName = "Bondi"
        var selectedIndex = -1
        val fc = FC {
            HeadingAndSelector {
                name = caseName
                numberOfCCs = 42
                selectCornerstone = { index ->
                    selectedIndex = index
                }
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                selectNextCornerstone()
                selectedIndex shouldBe 1
                selectNextCornerstone()
                selectedIndex shouldBe 2
            }
        }
    }

    @Test
    fun shouldSelectThePreviousCornerstoneIndex() {
        val caseName = "Bondi"
        var selectedIndex = -1
        val fc = FC {
            HeadingAndSelector {
                name = caseName
                numberOfCCs = 42
                selectCornerstone = { index ->
                    selectedIndex = index
                }
            }
        }
        runReactTest(fc) { container ->
            with(container) {
                (1..30).forEach { selectNextCornerstone() }
                selectedIndex shouldBe 30
                selectPreviousCornerstone()
                selectedIndex shouldBe 29
            }
        }
    }
}




