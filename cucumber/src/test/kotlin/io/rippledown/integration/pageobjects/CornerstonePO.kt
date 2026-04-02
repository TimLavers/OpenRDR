package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.cornerstone.CORNERSTONE_CASE_NAME_ID
import io.rippledown.integration.utils.find
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
}