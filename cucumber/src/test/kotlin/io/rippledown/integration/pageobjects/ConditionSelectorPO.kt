package io.rippledown.integration.pageobjects

//import io.rippledown.constants.interpretation.CONDITION_SELECTOR_CANCEL_BUTTON
//import io.rippledown.constants.interpretation.CONDITION_SELECTOR_DONE_BUTTON
//import io.rippledown.constants.interpretation.CONDITION_SELECTOR_ROW

// ORD2
class ConditionSelectorPO() {

    fun clickDone(): ConditionSelectorPO {
//        await.until {
//            driver.findElement(By.id(CONDITION_SELECTOR_DONE_BUTTON)).isEnabled
//        }
//        driver.findElement(By.id(CONDITION_SELECTOR_DONE_BUTTON)).click()
//        pause()
        return this
    }

    fun requireConditionsShowing(expectedConditions: List<String>): ConditionSelectorPO {
//        val found = driver.findElements(By.className("MuiFormControlLabel-root"))
//        found.size shouldBe expectedConditions.size
//        expectedConditions.forEachIndexed { index, condition ->
//            val row = driver.findElement(By.id("$CONDITION_SELECTOR_ROW$index"))
//            row.text shouldBe condition
//        }
        return this
    }

    fun requireConditionsToBeSelected(expectedConditions: List<String>) {
//        expectedConditions.forEachIndexed { index, condition ->
//            val row = driver.findElement(By.id("$CONDITION_SELECTOR_ROW$index"))
//            if (row.text == condition) {
//                row.getAttribute("class") shouldContain "Mui-checked"
//            }
//        }
    }


    fun clickConditionWithIndex(index: Int): ConditionSelectorPO {
//        driver.findElement(By.id("$CONDITION_SELECTOR_ROW$index")).click()
        return this
    }

    fun clickConditionWithText(condition: String): ConditionSelectorPO {
//        elementContainingText(condition).click()
        return this
    }


    fun clickCancel() {
//        driver.findElement(By.id(CONDITION_SELECTOR_CANCEL_BUTTON)).click()
    }
}