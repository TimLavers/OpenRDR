package io.rippledown.integration.pageobjects

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver

// ORD2
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

    fun referenceRange(attribute: String): String {
        val containerElement = containerElement()
        val idOfRangeCellForAttribute = "reference_range_cell_$attribute"
        val attributeValueCell = containerElement.findElement(By.id(idOfRangeCellForAttribute))
        return attributeValueCell!!.text
    }

    fun setInterpretationText(text: String) {
        val textArea = containerElement().findElement(By.id("interpretation_text_area"))
        textArea.sendKeys(text)
        val sendButton = containerElement().findElement(By.id("send_interpretation_button"))
        sendButton.click()
    }
}