package io.rippledown.integration.pageobjects

import io.rippledown.integration.pause
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver

// ORD2
class CaseViewPO(private val driver: WebDriver) {

    init {
        pause()//Need to wait for the case to render else we get a stale element. todo use a better mechanism
    }

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
        val textArea = interpretationArea()
        textArea.sendKeys(text)
        val sendButton = containerElement().findElement(By.id("send_interpretation_button"))
        sendButton.click()
    }

    fun interpretationText(): String {
        return interpretationArea().getAttribute("value")
    }

    private fun interpretationArea() = containerElement().findElement(By.id("interpretation_text_area"))
}