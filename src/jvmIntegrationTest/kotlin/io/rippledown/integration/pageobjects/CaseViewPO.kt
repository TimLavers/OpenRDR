package io.rippledown.integration.pageobjects

import io.rippledown.integration.pause
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.interactions.Actions
import kotlin.test.assertEquals

// ORD2
class CaseViewPO(private val driver: WebDriver) {

    init {
        pause()//Need to wait for the case to render else we get a stale element. todo use a better mechanism
    }

    private fun containerElement() = driver.findElement(By.id("case_view_container"))

    fun nameShown(): String {
        return containerElement().findElement(By.id("case_view_case_name")).text
    }

    fun datesShown(): List<String> {
        val result = mutableListOf<String>()

        val containerElement = containerElement()
        // Get the case header.
        val headerElement = containerElement.findElement(By.tagName("thead"))
        val valuesList = mutableListOf<String>()
        // Get all cells in the head.
        val rowCells = headerElement.findElements(By.tagName("th"))
        // First column and last columns are not dates.
        rowCells.forEachIndexed { index, cell ->
            if (index != 0 && index != rowCells.size - 1) {
                val cellId = "episode_date_cell_${index - 1}"
                assertEquals(cellId, cell.getAttribute("id")) //Sanity
                result.add(cell.text)
            }
        }
        return result
    }

    fun valuesShown(): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()

        val containerElement = containerElement()
        // Get the case body.
        val caseBodyElement = containerElement.findElement(By.tagName("tbody"))
        // For each row...
        caseBodyElement.findElements(By.tagName("tr")).forEach { rowElement ->
            val valuesList = mutableListOf<String>()
            // Get all cells in the row
            val rowCells = rowElement.findElements(By.tagName("td"))
            // First is the name.
            val attributeName = rowCells[0].text
            assertEquals(attributeCellId(attributeName), rowCells[0].getAttribute("id")) //Sanity check.
            // Last is the reference range. In between are the values.
            val numberOfEpisodes = rowCells.size - 2
            rowCells.forEachIndexed { index, cell ->
                if (index != 0 && index != rowCells.size - 1) {
                    val idOfValueCellForAttribute = "attribute_value_cell_${attributeName}_${index - 1}"
                    assertEquals(idOfValueCellForAttribute, cell.getAttribute("id")) //Sanity
                    val value = cell.text
                    valuesList.add(value)
                }
            }
            result[attributeName] = valuesList
        }
        return result
    }

    private fun attributeCellId(attributeName: String?) = "attribute_name_cell_$attributeName"

    fun dragAttribute(draggedAttribute: String, targetAttribute: String) {
        val dragId = attributeCellId(draggedAttribute)
        val draggedElement = driver.findElement(By.id(dragId))
        val targetId = attributeCellId(targetAttribute)
        val targetElement = driver.findElement(By.id(targetId))
        Actions(driver).dragAndDrop(draggedElement, targetElement)
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

    fun interpretationArea() = containerElement().findElement(By.id("interpretation_text_area"))
}