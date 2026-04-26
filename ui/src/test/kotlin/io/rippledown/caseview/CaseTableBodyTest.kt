package io.rippledown.caseview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.RDRCaseBuilder
import io.rippledown.model.caseview.CaseViewProperties
import io.rippledown.model.caseview.ViewableCase
import io.rippledown.utils.defaultDate
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Behavioural tests for [CaseTableBody]. The body renders just the attribute
 * rows (no dates header) and supports drag-and-drop reordering. Tests are
 * structured Given / When / Then.
 */
@OptIn(ExperimentalTestApi::class)
class CaseTableBodyTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var viewableCase: ViewableCase
    private val tsh = Attribute(1, "TSH")
    private val ft4 = Attribute(2, "FT4")
    private val abc = Attribute(3, "ABC")
    private val xyz = Attribute(4, "XYZ")
    private val clinicalNotes = Attribute(5, "Clinical Notes")

    @Before
    fun setup() {
        val builder = RDRCaseBuilder()
        builder.addValue(ft4, defaultDate, "12.8")
        builder.addValue(abc, defaultDate, "12.9")
        builder.addValue(xyz, defaultDate, "1.9")
        builder.addValue(tsh, defaultDate, "2.37")
        builder.addValue(clinicalNotes, defaultDate, "Lethargy.")
        val case = builder.build("Case1")
        val properties = CaseViewProperties(listOf(tsh, ft4, abc, xyz, clinicalNotes))
        viewableCase = ViewableCase(case, properties)
    }

    @Test
    fun `should show an attribute row for each attribute in the case`() = runTest {
        // Given a case with several attributes
        val columnWidths = ColumnWidths(viewableCase.numberOfColumns)

        // When the body is rendered
        composeTestRule.setContent {
            CaseTableBody(viewableCase, columnWidths)
        }

        // Then every attribute name is shown
        with(composeTestRule) {
            viewableCase.attributes().forEach {
                waitUntilExactlyOneExists(hasText(it.name))
            }
        }
    }

    @Test
    fun `should show the latest result for each attribute`() = runTest {
        // Given a case where each attribute has a single value
        val columnWidths = ColumnWidths(viewableCase.numberOfColumns)

        // When the body is rendered
        composeTestRule.setContent {
            CaseTableBody(viewableCase, columnWidths)
        }

        // Then each value is displayed alongside its attribute
        with(composeTestRule) {
            viewableCase.attributes().forEach {
                val value = viewableCase.case.getLatest(it)!!
                waitUntilExactlyOneExists(hasText(resultText(value)))
            }
        }
    }

    @Test
    fun `should report drag and drop reordering via the listener`() = runTest {
        // Given a body that records drag-and-drop callbacks
        var dragged: Attribute? = null
        var target: Attribute? = null
        val columnWidths = ColumnWidths(viewableCase.numberOfColumns)
        composeTestRule.setContent {
            CaseTableBody(viewableCase, columnWidths) { a, b ->
                dragged = a
                target = b
            }
        }

        // When the FT4 row is dragged onto the XYZ row
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(ft4.name))
            waitUntilExactlyOneExists(hasText(xyz.name))
            val ft4Bounds = onNodeWithText(ft4.name).getBoundsInRoot()
            val xyzBounds = onNodeWithText(xyz.name).getBoundsInRoot()
            onNodeWithText(ft4.name).performMouseInput {
                val relativeEnd = xyzBounds.center(density) - ft4Bounds.center(density)
                dragAndDrop(Offset(0f, 0f), relativeEnd)
            }
        }

        // Then the listener is told FT4 was the dragged attribute and XYZ the target
        dragged shouldBe ft4
        target shouldBe xyz
    }

    @Test
    fun `should not invoke the listener when no row is dragged`() = runTest {
        // Given a body that records drag-and-drop callbacks
        var listenerCalled = false
        val columnWidths = ColumnWidths(viewableCase.numberOfColumns)
        composeTestRule.setContent {
            CaseTableBody(viewableCase, columnWidths) { _, _ -> listenerCalled = true }
        }

        // When the body is simply rendered without any user interaction
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(tsh.name))
        }

        // Then the listener is never called
        listenerCalled shouldBe false
    }
}
