package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.cornerstone.CORNERSTONE_CASE_NAME_ID
import io.rippledown.constants.cornerstone.EXEMPT_BUTTON
import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_MSG
import io.rippledown.constants.navigation.INDEX_AND_TOTAL_ID
import io.rippledown.constants.navigation.NEXT_BUTTON
import io.rippledown.constants.navigation.OF
import io.rippledown.constants.navigation.PREVIOUS_BUTTON
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.waitForContextToBeNotNull
import io.rippledown.integration.waitUntilAsserted
import io.rippledown.main.INFO_MESSAGE_ID
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.time.Duration
import javax.accessibility.AccessibleContext

// ORD2
class CornerstonePO(private val contextProvider: () -> AccessibleContext) {

    fun requireMessageForNoCornerstones() {
        waitUntilAsserted {
            contextProvider().find(INFO_MESSAGE_ID)?.accessibleName shouldBe NO_CORNERSTONES_TO_REVIEW_MSG
        }
    }

    fun requireCornerstoneCase(expectedCaseName: String) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            val ccName = execute<String> { contextProvider().find(CORNERSTONE_CASE_NAME_ID)?.accessibleName }
            ccName shouldBe expectedCaseName
        }
    }

    fun requireCornerstoneCaseNotToBeShowing(ccName: String) {
        waitUntilAsserted {
            contextProvider().find(CORNERSTONE_CASE_NAME_ID)?.accessibleName shouldNotBe ccName
        }
    }

    fun selectNextCornerstoneCase() {
        waitForContextToBeNotNull(contextProvider, NEXT_BUTTON)
        execute { contextProvider().find(NEXT_BUTTON)!!.accessibleAction?.doAccessibleAction(0) }
    }

    fun selectPreviousCornerstoneCase() {
        waitForContextToBeNotNull(contextProvider, PREVIOUS_BUTTON)
        execute { contextProvider().find(PREVIOUS_BUTTON)!!.accessibleAction?.doAccessibleAction(0) }
    }

    fun exemptCornerstoneCase() {
        waitForContextToBeNotNull(contextProvider, EXEMPT_BUTTON)
        execute { contextProvider().find(EXEMPT_BUTTON)!!.accessibleAction?.doAccessibleAction(0) }
    }

    fun requireIndexAndNumberOfCornerstones(expectedIndex: Int, expectedNumberOfCornerstones: Int) {
        waitForContextToBeNotNull(contextProvider, INDEX_AND_TOTAL_ID)

        val contextForIndexAndTotal = execute<AccessibleContext?> { contextProvider().find(INDEX_AND_TOTAL_ID) }
        val indexAndTotal = contextForIndexAndTotal!!.accessibleName
        val (index, total) = indexAndTotal.split(" $OF ")
        index.toInt() shouldBe expectedIndex
        total.toInt() shouldBe expectedNumberOfCornerstones
    }
}