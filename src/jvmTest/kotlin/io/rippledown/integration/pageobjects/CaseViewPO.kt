package io.rippledown.integration.pageobjects

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver

class CaseViewPO(private val driver: WebDriver) {


    private fun containerElement() = driver.findElement(By.id("case_view_container"))

    fun nameShown(): String {
        return containerElement().findElement(By.id("case_view_case_name")).text
    }

    fun valuesShown(): Map<String, String> {
        val result = mutableMapOf<String, String>()

        val containerElement = containerElement()
        containerElement.findElements(By.tagName("td")).forEach {
            val id = it.getAttribute("id")
            if (id.startsWith("attribute_name_cell_")) {
                val attributeName = it.text
                val idOfValueCellForAttribute = "attribute_value_cell_$attributeName"
                val attributeValueCell = containerElement.findElement(By.id(idOfValueCellForAttribute))
                val attributeValue = attributeValueCell.text
                result[attributeName] = attributeValue
            }
        }

        return result
    }
}