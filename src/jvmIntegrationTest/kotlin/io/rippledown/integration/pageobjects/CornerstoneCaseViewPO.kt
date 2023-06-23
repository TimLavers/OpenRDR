package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.CASEVIEW_CORNERSTONE_CASE_NAME_ID
import io.rippledown.constants.interpretation.CASE_VIEW_CONTAINER
import io.rippledown.constants.interpretation.CORNERSTONE_VIEW_CONTAINER
import io.rippledown.constants.interpretation.EMPTY_CORNERSTONE_VIEW_CONTAINER
import io.rippledown.integration.pause
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import kotlin.test.assertEquals

// ORD2
class CornerstoneCaseViewPO(private val driver: WebDriver) {

    private fun cornerstoneContainerElement() = driver.findElement(By.id(CORNERSTONE_VIEW_CONTAINER))

    fun requireMessageForNoCornerstones(expected: String) {
        driver.findElement(By.id(EMPTY_CORNERSTONE_VIEW_CONTAINER)).text shouldBe expected
    }

    fun requireCornerstoneCase(expectedCaseName: String) {
        cornerstoneContainerElement().findElement(By.id(CASEVIEW_CORNERSTONE_CASE_NAME_ID)).text shouldBe expectedCaseName
    }
}