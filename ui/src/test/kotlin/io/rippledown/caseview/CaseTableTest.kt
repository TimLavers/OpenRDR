package io.rippledown.caseview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.width
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

@OptIn(ExperimentalTestApi::class)
class CaseTableTest {
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
        val case1 = builder.build("Case1")
        val properties = CaseViewProperties(listOf(tsh, ft4, abc, xyz, clinicalNotes))
        viewableCase = ViewableCase(case1, properties)
    }

    @Test
    fun show() = runTest {
        composeTestRule.setContent {
            CaseTable(viewableCase) { _: Attribute, _: Attribute -> }
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(tsh.name))
            viewableCase.attributes().forEach {
                val value = viewableCase.case.getLatest(it)!!
                waitUntilExactlyOneExists(hasText(resultText(value)))
            }
        }
    }

    @Test
    fun `drag and drop rows`() = runTest {
        var dragged: Attribute? = null
        var target: Attribute? = null
        composeTestRule.setContent {
            CaseTable(viewableCase) { a: Attribute, b: Attribute ->
                dragged = a
                target = b
            }
        }
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasText(tsh.name))
            viewableCase.attributes().forEach {
                val value = viewableCase.case.getLatest(it)!!
                waitUntilExactlyOneExists(hasText(resultText(value)))
            }
            val ft4Bounds = onNodeWithText(ft4.name).getBoundsInRoot()
            val xyzBounds = onNodeWithText(xyz.name).getBoundsInRoot()

            onNodeWithText(ft4.name).performMouseInput {
                // The dnd method takes its coordinates from the top left
                // corner of the node on which it operates. So our start point
                // can be (0, 0) and our end point is the displacement, which is the
                // difference between the offsets of the destination and source nodes.
                val relativeEnd = xyzBounds.center(density) - ft4Bounds.center(density)
                dragAndDrop(Offset(0F,0F), relativeEnd)
            }
            dragged shouldBe ft4
            target shouldBe xyz
        }
    }
}
fun DpRect.center(density: Float): Offset {
    val cx = (left.value + width.value/2) * density
    val cy = top.value * density
    return Offset(cx, cy)
}
