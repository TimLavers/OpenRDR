package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.constants.cornerstone.CORNERSTONE_CASE_NAME_ID
import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_MSG
import io.rippledown.constants.navigation.INDEX_AND_TOTAL_ID
import io.rippledown.constants.navigation.NEXT_BUTTON
import io.rippledown.constants.navigation.OF
import io.rippledown.constants.navigation.PREVIOUS_BUTTON
import io.rippledown.integration.utils.find
import io.rippledown.integration.waitUntilAssertedOnEventThread
import io.rippledown.main.INFO_MESSAGE_ID
import org.assertj.swing.edt.GuiActionRunner.execute
import javax.accessibility.AccessibleContext

// ORD2
class CornerstonePO(private val contextProvider: () -> AccessibleContext) {

    fun requireMessageForNoCornerstones() {
        waitUntilAssertedOnEventThread {
            contextProvider().find(INFO_MESSAGE_ID)?.accessibleName shouldBe NO_CORNERSTONES_TO_REVIEW_MSG
        }
    }

    fun requireCornerstoneCase(expectedCaseName: String) {
        waitUntilAssertedOnEventThread {
            contextProvider().find(CORNERSTONE_CASE_NAME_ID)?.accessibleName shouldBe expectedCaseName
        }
    }

    fun selectNextCornerstoneCase() {
        execute { contextProvider().find(NEXT_BUTTON)!!.accessibleAction?.doAccessibleAction(0) }
    }

    fun selectPreviousCornerstoneCase() {
        execute { contextProvider().find(PREVIOUS_BUTTON)!!.accessibleAction?.doAccessibleAction(0) }
    }

    fun requireIndexAndNumberOfCornerstones(expectedIndex: Int, expectedNumberOfCornerstones: Int) {
        waitUntilAssertedOnEventThread {
            val contextForIndexAndTotal = contextProvider().find(INDEX_AND_TOTAL_ID)
            contextForIndexAndTotal shouldNotBe null

            val indexAndTotal = contextForIndexAndTotal!!.accessibleName
            val (index, total) = indexAndTotal.split(" $OF ")
            index.toInt() shouldBe expectedIndex
            total.toInt() shouldBe expectedNumberOfCornerstones
        }
    }
}