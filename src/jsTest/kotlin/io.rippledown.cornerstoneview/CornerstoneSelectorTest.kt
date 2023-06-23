package io.rippledown.cornerstoneview

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import proxy.debug
import proxy.waitForEvents
import react.VFC
import react.dom.checkContainer
import react.dom.createRootFor
import kotlin.test.Test

class CornerstoneSelectorTest {
    @Test
    fun shouldDefaultToFirstCornerstoneIndex() = runTest {
        val vfc = VFC {
            CornerstoneSelector {
                total = 42
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireSelectedCornerstoneIndex(1)
            }
        }
    }
 @Test
    fun shouldTruncateLongNames() = runTest {
        val vfc = VFC {
            CornerstoneSelector {
                total = 42
            }
        }
        checkContainer(vfc) { container ->
            with(container) {
                requireSelectedCornerstoneIndex(1)
            }
        }
    }

    @Test
    fun shouldBeAbleToSelectTheNextCornerstoneIndex() = runTest {
        var selectedIndex = -1
        val vfc = VFC {
            CornerstoneSelector {
                total = 42
                onSelect = { index ->
                    selectedIndex = index
                    debug("handler received index $index")
                }
            }
        }
        val container = createRootFor(vfc)
        with(container) {
            selectNextCornerstone()
            waitForEvents()
            requireSelectedCornerstoneIndex(2)
            selectNextCornerstone()
            waitForEvents()
            requireSelectedCornerstoneIndex(3)
            selectNextCornerstone()
            waitForEvents()
            requireSelectedCornerstoneIndex(4)
                selectedIndex shouldBe 4
        }
    }


@Test
fun shouldBeAbleToSelectThePreviousCornerstoneIndex() = runTest {
    var selectedIndex = -1
    val vfc = VFC {
        CornerstoneSelector {
            total = 42
            onSelect = { index ->
                selectedIndex = index
            }
        }
    }
    val container = createRootFor(vfc)
        with(container) {
            selectNextCornerstone()
            selectNextCornerstone()
            selectNextCornerstone()
            waitForEvents()
            requireSelectedCornerstoneIndex(4)
            selectPreviousCornerstone()
            selectPreviousCornerstone()
            selectPreviousCornerstone()
            waitForEvents()
            requireSelectedCornerstoneIndex(1)
            selectedIndex shouldBe 1
        }
    }

}



