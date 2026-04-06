package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.cornerstone.*
import io.rippledown.constants.navigation.NEXT_BUTTON
import io.rippledown.constants.navigation.PREVIOUS_BUTTON
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAndClick
import io.rippledown.integration.waitUntilAsserted
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration
import javax.accessibility.AccessibleContext

// ORD2
class CornerstonePO(private val contextProvider: () -> AccessibleContext) {

    fun requireCornerstoneCase(expectedCaseName: String) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            val ccName = execute<String> { contextProvider().find(CORNERSTONE_CASE_NAME_ID)?.accessibleName }
            ccName shouldBe expectedCaseName
        }
    }

    fun requireNoCornerstoneCases() {
        waitUntilAsserted {
            contextProvider().find(CORNERSTONE_CASE_NAME_ID) shouldBe null
        }
    }

    fun requireCornerstoneCaseNotToBeShowing(ccName: String) {
        waitUntilAsserted {
            contextProvider().find(CORNERSTONE_CASE_NAME_ID)?.accessibleName shouldNotBe ccName
        }
    }

    fun requireCornerstoneLabel(expectedLabel: String) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            val label = execute<String> { contextProvider().find(CORNERSTONE_ID)?.accessibleName }
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