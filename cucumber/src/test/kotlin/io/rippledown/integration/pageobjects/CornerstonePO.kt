package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe

import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_ID
import io.rippledown.constants.cornerstone.NO_CORNERSTONES_TO_REVIEW_MSG
import io.rippledown.integration.utils.find
import io.rippledown.integration.waitUntilAssertedOnEventThread
import javax.accessibility.AccessibleContext

// ORD2
class CornerstonePO(private val contextProvider: () -> AccessibleContext) {

    fun requireMessageForNoCornerstones() {
        waitUntilAssertedOnEventThread {
            contextProvider().find(NO_CORNERSTONES_TO_REVIEW_ID)?.accessibleName shouldBe NO_CORNERSTONES_TO_REVIEW_MSG
        }
    }

    fun requireCornerstoneCase(expectedCaseName: String) {
//        cornerstoneContainerElement().findElement(By.id(CASEVIEW_CORNERSTONE_CASE_NAME_ID)).text shouldBe expectedCaseName
    }

    fun selectNextCornerstoneCase() {
//        cornerstoneContainerElement()
//            .findElement(By.cssSelector("[aria-label='Go to next page']")).click()
    }

    fun selectPreviousCornerstoneCase() {
//        cornerstoneContainerElement()
//            .findElement(By.cssSelector("[aria-label='Go to previous page']")).click()
    }

    fun requireNumberOfCornerstones(expectedNumberOfCornerstones: Int) {
//        val listElements = cornerstoneContainerElement().findElements(By.tagName("li"))

        //first and last list elements are the navigation buttons
//        listElements.size - 2 shouldBe expectedNumberOfCornerstones
    }
}