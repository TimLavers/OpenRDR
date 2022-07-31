package io.rippledown.integration.pageobjects

import io.rippledown.integration.pause
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver

class CaseListPO(private val driver: WebDriver) {

    fun isShowing(): Boolean {
        return containerElement() != null
    }

    private fun containerElement() = driver.findElement(By.id("case_list_container"))

    fun casesListed(): List<String> {
        return containerElement().findElements(By.tagName("li")).map { it.text }
    }

    fun select(caseName: String): CaseViewPO {
        val id = "case_list_item_$caseName"
        driver.findElement(By.id(id)).click()
        return CaseViewPO(driver)
    }
}