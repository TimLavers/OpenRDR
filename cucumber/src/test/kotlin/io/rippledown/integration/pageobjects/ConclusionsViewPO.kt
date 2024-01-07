package io.rippledown.integration.pageobjects

import io.rippledown.constants.interpretation.INTERPRETATION_TAB_CONCLUSIONS

class ConclusionsViewPO() {

    fun selectConclusionsTab() {
//        driver.findElement(By.id(INTERPRETATION_TAB_CONCLUSIONS)).click()
    }

    fun clickClose() {
//        driver.findElement(By.id("conclusions_dialog_close")).click()
    }

    fun clickComment(comment: String) {
//        driver.findElement(By.ByXPath("//*[contains(@id, '$comment')]")).click()
    }

    fun requireConditionsToBeShown(vararg conditions: String) {
//        conditions.forEach { condition ->
//            driver.findElement(By.ByXPath("//*[contains(@id, '$condition')]"))
//        }
    }
}