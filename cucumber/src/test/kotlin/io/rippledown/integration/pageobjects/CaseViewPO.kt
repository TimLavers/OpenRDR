package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.rippledown.caseview.attributeCellContentDescriptionPrefix
import io.rippledown.caseview.valueCellContentDescriptionPrefix
import io.rippledown.constants.caseview.*
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAllByDescriptionPrefix
import io.rippledown.integration.utils.findAndClick
import org.assertj.swing.edt.GuiActionRunner.execute
import org.awaitility.Awaitility.await
import java.awt.Point
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole.LABEL

// ORD2
class CaseViewPO(private val contextProvider: () -> AccessibleContext) {

    init {
        //Awaits in waitForNameToShow and waitForRequiredCaseValues handle rendering delays
    }

    fun nameShown(): String? = execute<String> {
        // Find the case-name label by its contentDescription prefix and recover
        // the case name from the suffix. From Compose 1.11 the Java accessibility
        // bridge uses contentDescription as the accessible name on Text nodes,
        // overriding the rendered text, so we cannot rely on accessibleName.
        val matcher: (AccessibleContext) -> Boolean = { ctx ->
            ctx.accessibleRole == LABEL &&
                    ctx.accessibleDescription?.startsWith(CASEVIEW_CASE_NAME_ID) == true
        }
        contextProvider().find(matcher)
            ?.accessibleDescription
            ?.removePrefix(CASEVIEW_CASE_NAME_ID)
            ?.takeIf { it.isNotEmpty() }
    }

    private fun awaitNameShown(): String {
        await().atMost(ofSeconds(10)).until { nameShown() != null }
        return nameShown()!!
    }

    fun requireNoNameShowing() {
        nameShown() shouldBe null
    }

    fun waitForNoNameShowing() {
        await().atMost(ofSeconds(5)).until {
            nameShown() == null
        }
    }

    fun waitForNameToShow(name: String) {
        await().atMost(ofSeconds(10)).until {
            nameShown() != null && nameShown() == name
        }
    }

    fun datesShown() =
        extractMatchingValuesInOrderShown(DATE_CELL_DESCRIPTION_PREFIX) { context -> DateCellPO(context) }

    fun attributeNames(): List<String> {
        return execute<List<String>> {
            val table = contextProvider().find(CASE_VIEW_TABLE) ?: return@execute emptyList()
            table
                .findAllByDescriptionPrefix("") // Find all elements with content description
                .filter { context ->
                    // Filter for attribute cells by checking if they have the expected structure
                    // and don't match other known prefixes
                    val description = context.accessibleName
                    description.isNotBlank() &&
                            context.accessibleRole == LABEL && // Only get LABEL elements (text)
                            // Must be a single word that starts with uppercase letter and contains only letters
                            description.matches(Regex("^[A-Z][a-zA-Z]*$")) &&
                            // Exclude known non-attribute patterns
                            !description.startsWith("Date") &&
                            !description.startsWith("Reference") &&
                            !description.startsWith("Out") &&
                            !description.startsWith("Units") &&
                            !description.startsWith("Filter") &&
                            !description.startsWith("Clear") &&
                            !description.startsWith("Case") &&
                            !description.startsWith("Attributes") &&
                            !description.contains("value") &&
                            !description.contains("column") &&
                            !description.contains("table") &&
                            !description.contains("filter") &&
                            description.length <= 20 // Reasonable length limit for attribute names
                }
                .map { it.accessibleName }
                .sorted()
        }
    }

    fun valuesForAttribute(attribute: String): List<String> {
        val caseName = awaitNameShown()
        return execute<List<String>> {
            val table = contextProvider().find(CASE_VIEW_TABLE) ?: return@execute emptyList()
            table
                .findAllByDescriptionPrefix("") // Find all elements with content description
                .filter { context ->
                    // Look for value cells for the specific attribute
                    val description = context.accessibleName
                    description.isNotBlank() &&
                            context.accessibleRole == LABEL &&
                            !description.startsWith("Date for episode") &&
                            !description.startsWith("Reference range for") &&
                            !description.startsWith("Out of range marker for") &&
                            !description.startsWith("Units for") &&
                            !description.startsWith("Filter ") &&
                            !description.startsWith("Clear filter") &&
                            description != "Attributes column" &&
                            description != "Reference ranges column" &&
                            description != "Units column" &&
                            description != "Case view table" &&
                            description != "Filter" &&
                            description != "Filter attributes or values" &&
                            // Check if this looks like a value (numeric or contains the case name)
                            (description.matches(Regex("^[<>]?\\d+(\\.\\d+)?$")) || // Numeric value
                                    description.contains(caseName) || // Contains case name (fallback)
                                    description.matches(Regex("^[+-]?\\d+(\\.\\d+)?[eE][+-]?\\d+$"))) // Scientific notation
                }
                .map { it.accessibleName }
                .filter { it.isNotBlank() }
                .sorted()
        }
    }

    fun valuesShown(): LinkedHashMap<String, List<String>> {
        val result = LinkedHashMap<String, List<String>>()
        attributeNames().forEach { attribute ->
            result[attribute] = valuesForAttribute(attribute)
        }
        return result
    }

    private fun getAttributeCellsInOrderShown(): List<AttributeCellPO> {
        val caseName = awaitNameShown()
        val contentDescriptionPrefix = attributeCellContentDescriptionPrefix(caseName)
        return contextProvider()
            .find(CASE_VIEW_TABLE)!!//narrow down the context to the table
            .findAllByDescriptionPrefix(contentDescriptionPrefix)
            .map { AttributeCellPO(it, caseName) }
            .sorted()
    }

    private fun extractMatchingValuesInOrderShown(
        descriptionPrefix: String,
        contextToPO: (AccessibleContext) -> CellPO
    ): List<String> = execute<List<String>> {
        //The table may not yet be rendered when this runs inside an awaitility
        //retry (e.g. right after switching to a newly-created KB in the Zoo
        //sample scenario). Returning an empty list lets the caller re-poll
        //instead of throwing an NPE that would escape the retry harness.
        val table = contextProvider().find(CASE_VIEW_TABLE) ?: return@execute emptyList()
        table
            .findAllByDescriptionPrefix(descriptionPrefix)
            .map { contextToPO(it) }
            .sorted()
            .map { it.text() }
    }

    private fun attributeCellId(attributeName: String?) = "attribute_name_cell_$attributeName"

    fun dragAttribute(draggedAttribute: String, targetAttribute: String) {
        val allAttributeCells = getAttributeCellsInOrderShown()
        fun cellPosition(attribute: String): Point {
            val draggedCell = allAttributeCells.find { it.text() == attribute }!!
            val accessibleComponent = draggedCell.context.accessibleComponent
            val topLeft = accessibleComponent.locationOnScreen
            return Point(topLeft.x + 5, topLeft.y + 5)
        }
        val targetPos = cellPosition(targetAttribute)
        val rowSpacing = if (allAttributeCells.size >= 2) {
            cellPosition(allAttributeCells[1].text()).y - cellPosition(allAttributeCells[0].text()).y
        } else {
            30
        }
        dragVertically(cellPosition(draggedAttribute), Point(targetPos.x, targetPos.y + rowSpacing / 2))
    }

    // Returns "" rather than throwing NPE if the cell is not yet present. This
    // method is typically called inside an awaitility `untilAsserted` retry,
    // which only catches AssertionError — not NPE
    fun referenceRange(attribute: String): String = execute<String> {
        val table = contextProvider().find(CASE_VIEW_TABLE) ?: return@execute ""
        table
            .findAllByDescriptionPrefix("") // Find all elements with content description
            .filter { context ->
                // Look for reference range cells - they contain the actual reference range text
                val description = context.accessibleName
                description.isNotBlank() &&
                        context.accessibleRole == LABEL &&
                        (description.matches(Regex("^[<>]?\\d+(\\.\\d+)?\\s*-\\s*\\d+(\\.\\d+)?$")) || // "1.2 - 3.4"
                                description.matches(Regex("^[<>]\\s*\\d+(\\.\\d+)?$")) || // "< 5.6" or "> 7.8"
                                description.startsWith("Reference range for")) // Fallback for empty ranges
            }
            .map { it.accessibleName }
            .firstOrNull { it.isNotBlank() && !it.startsWith("Reference range for") }
            .orEmpty()
    }

    /**
     * Replaces the case-view filter field's contents with [text]. The same
     * filter governs the current case and any cornerstone case displayed
     * alongside it; see [io.rippledown.casecontrol.CaseViewFilterField].
     */
    fun enterFilter(text: String) {
        await().atMost(ofSeconds(5)).until {
            contextProvider().find(CASE_VIEW_FILTER_FIELD_DESCRIPTION) != null
        }
        val field = contextProvider().find(CASE_VIEW_FILTER_FIELD_DESCRIPTION)!!
        execute {
            field.accessibleEditableText.setTextContents(text)
        }
    }

    /**
     * Clicks the filter's clear (×) button. The button is rendered only when
     * the filter has a value, so this assumes a non-empty filter is active.
     */
    fun clearFilter() {
        contextProvider().findAndClick(CASE_VIEW_FILTER_CLEAR_DESCRIPTION)
    }
}

open class CellPO(val context: AccessibleContext, descriptionPrefix: String) : Comparable<CellPO> {
    private val index = context.accessibleDescription.substring(descriptionPrefix.length).trim().toInt()

    override fun compareTo(other: CellPO) = index.compareTo(other.index)

    fun text() = context.accessibleName ?: ""
}

class DateCellPO(context: AccessibleContext) : CellPO(context, DATE_CELL_DESCRIPTION_PREFIX)
class AttributeCellPO(context: AccessibleContext, caseName: String) :
    CellPO(context, attributeCellContentDescriptionPrefix(caseName))

class ValueCellPO(context: AccessibleContext, caseName: String, attribute: String) : CellPO(
    context, valueCellContentDescriptionPrefix(
        caseName, attribute
    )
)