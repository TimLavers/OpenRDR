package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.rippledown.caseview.referenceRangeCellContentDescription
import io.rippledown.constants.caseview.ATTRIBUTE_CELL_DESCRIPTION_PREFIX
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID
import io.rippledown.constants.caseview.DATE_CELL_DESCRIPTION_PREFIX
import io.rippledown.constants.caseview.REFERENCE_RANGE_CELL_DESCRIPTION_PREFIX
import io.rippledown.integration.pause
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAllByDescriptionPrefix
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole
import javax.accessibility.AccessibleRole.LABEL

// ORD2
class CaseViewPO(private val contextProvider: () -> AccessibleContext) {

    init {
        pause()//Need to wait for the case to render else we get a stale element. todo use a better mechanism
    }

//    private fun caseContainerElement() = driver.findElement(By.id(CASE_VIEW_CONTAINER))

    fun nameShown(): String = contextProvider().find(CASEVIEW_CASE_NAME_ID, LABEL)!!.accessibleName

    fun requireNoNameShowing(){
         contextProvider().find(CASEVIEW_CASE_NAME_ID, LABEL)  shouldBe null
    }

    fun datesShown() = extractMatchingValuesInOrderShown(DATE_CELL_DESCRIPTION_PREFIX) { context -> DateCellPO(context) }

    fun attributeNames() = extractMatchingValuesInOrderShown(ATTRIBUTE_CELL_DESCRIPTION_PREFIX) { context -> AttributeCellPO(context)}

    fun valuesForAttribute(attribute: String) = extractMatchingValuesInOrderShown("$attribute value"){context -> ValueCellPO(context, attribute) }

    fun valuesShown(): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()

//        val containerElement = caseContainerElement()
        // Get the case body.
//        val caseBodyElement = containerElement.findElement(By.tagName("tbody"))
        // For each row...
//        caseBodyElement.findElements(By.tagName("tr")).forEach { rowElement ->
//            val valuesList = mutableListOf<String>()
            // Get all cells in the row
//            val rowCells = rowElement.findElements(By.tagName("td"))
            // First is the name.
//            val attributeName = rowCells[0].text
//            assertEquals(attributeCellId(attributeName), rowCells[0].getAttribute("id")) //Sanity check.
            // Last is the reference range. In between are the values.
//            rowCells.forEachIndexed { index, cell ->
//                if (index != 0 && index != rowCells.size - 1) {
//                    val idOfValueCellForAttribute = "attribute_value_cell_${attributeName}_${index - 1}"
//                    assertEquals(idOfValueCellForAttribute, cell.getAttribute("id")) //Sanity
//                    val value = cell.text
//                    valuesList.add(value)
//                }
//            }
//            result[attributeName] = valuesList
//        }
        return result
    }

    private fun extractMatchingValuesInOrderShown(descriptionPrefix: String, contextToPO: (AccessibleContext) -> (CellPO)) = contextProvider()
        .findAllByDescriptionPrefix(descriptionPrefix)
        .map {contextToPO(it) }
        .sorted()
        .map { it.text() }

    private fun attributeCellId(attributeName: String?) = "attribute_name_cell_$attributeName"

    fun dragAttribute(draggedAttribute: String, targetAttribute: String) {
//        DnD(driver).dragAttribute(draggedAttribute, targetAttribute)
    }

    fun referenceRange(attribute: String): String = contextProvider()
        .find(referenceRangeCellContentDescription(attribute), LABEL)!!
        .accessibleName
}
open class CellPO(private val context: AccessibleContext, descriptionPrefix: String): Comparable<CellPO> {
    private val index = context.accessibleDescription.substring(descriptionPrefix.length).trim().toInt()

    override fun compareTo(other: CellPO) = index.compareTo(other.index)

    fun text(): String = context.accessibleName
}
class DateCellPO(context: AccessibleContext): CellPO(context, DATE_CELL_DESCRIPTION_PREFIX)
class AttributeCellPO(context: AccessibleContext): CellPO(context, ATTRIBUTE_CELL_DESCRIPTION_PREFIX)
class ValueCellPO(context: AccessibleContext, attribute: String): CellPO(context, "$attribute value")
class ReferenceRangeCellPO(context: AccessibleContext, attribute: String): CellPO(context, "$REFERENCE_RANGE_CELL_DESCRIPTION_PREFIX $attribute")