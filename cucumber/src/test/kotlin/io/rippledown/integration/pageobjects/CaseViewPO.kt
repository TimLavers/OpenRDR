package io.rippledown.integration.pageobjects

import io.kotest.matchers.shouldBe
import io.rippledown.caseview.attributeCellContentDescriptionPrefix
import io.rippledown.caseview.referenceRangeCellContentDescription
import io.rippledown.caseview.valueCellContentDescriptionPrefix
import io.rippledown.constants.caseview.CASEVIEW_CASE_NAME_ID
import io.rippledown.constants.caseview.CASE_VIEW_TABLE
import io.rippledown.constants.caseview.DATE_CELL_DESCRIPTION_PREFIX
import io.rippledown.integration.pause
import io.rippledown.integration.utils.find
import io.rippledown.integration.utils.findAllByDescriptionPrefix
import org.awaitility.kotlin.await
import java.time.Duration.ofSeconds
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole.LABEL

// ORD2
class CaseViewPO(private val contextProvider: () -> AccessibleContext) {

    init {
        pause()//Need to wait for the case to render else we get a stale element. todo use a better mechanism
    }

    fun nameShown(): String? = contextProvider().find(CASEVIEW_CASE_NAME_ID, LABEL)?.accessibleName

    fun requireNoNameShowing() {
        contextProvider().find(CASEVIEW_CASE_NAME_ID, LABEL) shouldBe null
    }

    fun waitForNoNameShowing() {
        await.atMost(ofSeconds(5)).until {
            contextProvider().find(CASEVIEW_CASE_NAME_ID, LABEL) == null
        }
    }

    fun waitForNameToShow(name: String) {
        await.atMost(ofSeconds(10)).until {
            nameShown() != null && nameShown() == name
        }
    }

    fun datesShown() =
        extractMatchingValuesInOrderShown(DATE_CELL_DESCRIPTION_PREFIX) { context -> DateCellPO(context) }

    fun attributeNames(): List<String> {
        val caseName = nameShown()!!
        val contentDescriptionPrefix = attributeCellContentDescriptionPrefix(caseName)
        return extractMatchingValuesInOrderShown(contentDescriptionPrefix) { context ->
            AttributeCellPO(
                context,
                caseName
            )
        }
    }

    fun valuesForAttribute(attribute: String): List<String> {
        val caseName = nameShown()!!
        val contentDescriptionPrefix = valueCellContentDescriptionPrefix(caseName, attribute)
        return extractMatchingValuesInOrderShown(contentDescriptionPrefix) { context ->
            ValueCellPO(
                context,
                caseName,
                attribute
            )
        }
    }

    fun valuesShown(): LinkedHashMap<String, List<String>> {
        val result = LinkedHashMap<String, List<String>>()
        attributeNames().forEach { attribute ->
            result[attribute] = valuesForAttribute(attribute)
        }
        return result
    }

    private fun extractMatchingValuesInOrderShown(
        descriptionPrefix: String,
        contextToPO: (AccessibleContext) -> CellPO
    ): List<String> = contextProvider()
        .find(CASE_VIEW_TABLE)!!//narrow down the context to the table
        .findAllByDescriptionPrefix(descriptionPrefix)
        .map { contextToPO(it) }
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

open class CellPO(private val context: AccessibleContext, descriptionPrefix: String) : Comparable<CellPO> {
    private val index = context.accessibleDescription.substring(descriptionPrefix.length).trim().toInt()

    override fun compareTo(other: CellPO) = index.compareTo(other.index)

    fun text(): String = context.accessibleName
}

class DateCellPO(context: AccessibleContext) : CellPO(context, DATE_CELL_DESCRIPTION_PREFIX)
class AttributeCellPO(context: AccessibleContext, caseName: String) :
    CellPO(context, attributeCellContentDescriptionPrefix(caseName))

class ValueCellPO(context: AccessibleContext, caseName: String, attribute: String) : CellPO(
    context, valueCellContentDescriptionPrefix(
        caseName, attribute
    )
)