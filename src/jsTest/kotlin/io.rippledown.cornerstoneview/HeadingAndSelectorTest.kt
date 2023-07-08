package io.rippledown.cornerstoneview

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import react.VFC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test

class HeadingAndSelectorTest {
    @Test
    fun shouldShowTheSpecifiedCornerstoneName() = runTest {
        val caseName = "Bondi"
        val vfc = VFC {
            HeadingAndSelector {
                name = caseName
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireCornerstoneCaseToBeShowing(caseName)
            }
        }
    }

    @Test
    fun shouldSelectTheNextCornerstoneIndex() = runTest {
        val caseName = "Bondi"
        var selectedIndex = -1
        val vfc = VFC {
            HeadingAndSelector {
                name = caseName
                numberOfCCs = 42
                selectCornerstone = { index ->
                    selectedIndex = index
                }
            }
        }
        with(createRootFor(vfc)) {
            selectNextCornerstone()
            selectedIndex shouldBe 1
            selectNextCornerstone()
            selectedIndex shouldBe 2
        }
    }

    @Test
    fun shouldSelectThePreviousCornerstoneIndex() = runTest {
        val caseName = "Bondi"
        var selectedIndex = -1
        val vfc = VFC {
            HeadingAndSelector {
                name = caseName
                numberOfCCs = 42
                selectCornerstone = { index ->
                    selectedIndex = index
                }
            }
        }
        with(createRootFor(vfc)) {
            (1..30).forEach { selectNextCornerstone() }
            selectedIndex shouldBe 30
            selectPreviousCornerstone()
            selectedIndex shouldBe 29
        }
    }
}




