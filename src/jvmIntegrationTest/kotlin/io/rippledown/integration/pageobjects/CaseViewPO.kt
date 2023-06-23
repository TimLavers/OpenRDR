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
class CaseViewPO(private val driver: WebDriver) {

    init {
        pause()//Need to wait for the case to render else we get a stale element. todo use a better mechanism
    }

    private fun caseContainerElement() = driver.findElement(By.id(CASE_VIEW_CONTAINER))

    fun nameShown(): String {
        return caseContainerElement().findElement(By.id("case_view_case_name")).text
    }

    fun noNameShowing(): Boolean {
        return driver.findElements(By.id(CASE_VIEW_CONTAINER)).size == 0
    }

    fun datesShown(): List<String> {
        val result = mutableListOf<String>()

        val containerElement = caseContainerElement()
        // Get the case header.
        val headerElement = containerElement.findElement(By.tagName("thead"))
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

    fun attributes(): List<String> {
        val result = mutableListOf<String>()
        val containerElement = caseContainerElement()
        val caseBodyElement = containerElement.findElement(By.tagName("tbody"))
        caseBodyElement.findElements(By.tagName("tr")).forEach { rowElement ->
            val rowCells = rowElement.findElements(By.tagName("td"))
            result.add(rowCells[0].text)
        }
        return result
    }

    fun valuesShown(): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()

        val containerElement = caseContainerElement()
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
        DnD(driver).dragAttribute(draggedAttribute, targetAttribute)
    }

    fun referenceRange(attribute: String): String {
        val containerElement = caseContainerElement()
        val idOfRangeCellForAttribute = "reference_range_cell_$attribute"
        val attributeValueCell = containerElement.findElement(By.id(idOfRangeCellForAttribute))
        return attributeValueCell!!.text
    }


}