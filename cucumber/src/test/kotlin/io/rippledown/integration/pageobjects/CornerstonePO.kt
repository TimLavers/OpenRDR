package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.cornerstone.CORNERSTONE_TITLE
import io.rippledown.constants.cornerstone.EXEMPT_BUTTON
import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_ID
import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_MSG
import io.rippledown.constants.navigation.NEXT_BUTTON
import io.rippledown.constants.navigation.PREVIOUS_BUTTON
import io.rippledown.cornerstone.CornerstoneTestHook
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration
import javax.accessibility.AccessibleContext

// ORD2
class CornerstonePO(private val contextProvider: () -> AccessibleContext) {

    // The next four polling helpers read from [CornerstoneTestHook] —
    // an in-JVM observation surface populated by `OpenRDRUI` on every
    // recomposition. This avoids walking the Compose accessibility
    // tree, which on a window with a large case table costs ~6 s per
    // call and not only wastes wall-clock time but also starves the
    // dispatcher coroutine that is supposed to refresh the current
    // case after a rule commit. See `CornerstoneTestHook` and
    // `ChatTestHook` for full context.
    fun requireCornerstoneCase(expectedCaseName: String) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            val ccName = POTiming.time("CornerstonePO.requireCornerstoneCase.poll") {
                CornerstoneTestHook.snapshot().cornerstoneCaseName
            }
            ccName shouldBe expectedCaseName
        }
    }

    fun requireNoCornerstoneCases() {
        waitUntilAsserted {
            val isShowing = POTiming.time("CornerstonePO.requireNoCornerstoneCases.poll") {
                CornerstoneTestHook.snapshot().isShowing
            }
            isShowing shouldBe false
        }
    }

    fun requireCornerstoneCaseNotToBeShowing(ccName: String) {
        waitUntilAsserted {
            val name = POTiming.time("CornerstonePO.requireCornerstoneCaseNotToBeShowing.poll") {
                CornerstoneTestHook.snapshot().cornerstoneCaseName
            }
            name shouldNotBe ccName
        }
    }

    fun requireCornerstoneLabel(expectedLabel: String) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            val label = POTiming.time("CornerstonePO.requireCornerstoneLabel.poll") {
                val s = CornerstoneTestHook.snapshot()
                if (s.isShowing) "$CORNERSTONE_TITLE ${s.indexOfCornerstoneToReview} of ${s.numberOfCornerstones}" else null
            }
            label shouldBe expectedLabel
        }
    }

    fun requireCornerstoneIndicator(index: Int, total: Int) {
        requireCornerstoneLabel("$CORNERSTONE_TITLE $index of $total")
    }

    fun clickNextButton() {
        execute { contextProvider().findAndClick(NEXT_BUTTON) }
    }

    fun clickPreviousButton() {
        execute { contextProvider().findAndClick(PREVIOUS_BUTTON) }
    }

    fun clickExemptButton() {
        execute { contextProvider().findAndClick(EXEMPT_BUTTON) }
    }

    fun requireNoCornerstonesToReviewMessage() {
        waitUntilAsserted {
            val message = execute<String> { contextProvider().find(NO_CORNERSTONES_TO_REVIEW_ID)?.accessibleName }
            message shouldBe NO_CORNERSTONES_TO_REVIEW_MSG
        }
    }
}