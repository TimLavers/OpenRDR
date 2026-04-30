package io.rippledown.casecontrol

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import io.rippledown.constants.caseview.CASE_VIEW_SCROLL_BAR
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [CaseInspectionLayout]. The layout pins a header at the top and
 * an interpretation slot below the body. When the body fits, the
 * interpretation hugs it from below; when it does not, the body scrolls
 * internally and the interpretation stays pinned just above the bottom edge
 * of the panel.
 *
 * Each test follows a Given / When / Then structure and uses tagged Box
 * stand-ins as the three slots so we can assert their absolute positions
 * deterministically.
 */
@OptIn(ExperimentalTestApi::class)
class CaseInspectionLayoutTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private val headerTag = "header-slot"
    private val bodyTag = "body-slot"
    private val interpretationTag = "interpretation-slot"

    private val panelWidth = 400.dp
    private val panelHeight = 300.dp
    private val headerHeight = 30.dp
    private val interpretationHeight = 40.dp

    @Test
    fun `should place the header at the top of the panel`() {
        // Given a panel hosting the layout
        composeTestRule.setContent {
            Box(modifier = Modifier.size(panelWidth, panelHeight)) {
                CaseInspectionLayout(
                    caseHeader = { headerSlot() },
                    caseBody = { bodySlot(heightDp = 60) },
                    interpretationContent = { interpretationSlot() }
                )
            }
        }

        // When the layout has been measured
        with(composeTestRule) {
            // Then the header is rendered at the top of the panel
            val headerBounds = onNodeWithTag(headerTag).getBoundsInRoot()
            headerBounds.top shouldBe 0.dp
        }
    }

    @Test
    fun `should hug the interpretation directly under the body when content fits`() {
        // Given a small body that comfortably fits inside the panel
        val smallBodyHeight = 60
        composeTestRule.setContent {
            Box(modifier = Modifier.size(panelWidth, panelHeight)) {
                CaseInspectionLayout(
                    caseHeader = { headerSlot() },
                    caseBody = { bodySlot(heightDp = smallBodyHeight) },
                    interpretationContent = { interpretationSlot() }
                )
            }
        }

        // When the layout has been measured
        with(composeTestRule) {
            // Then the body sits directly below the header
            val headerBounds = onNodeWithTag(headerTag).getBoundsInRoot()
            val bodyBounds = onNodeWithTag(bodyTag).getBoundsInRoot()
            val interpretationBounds = onNodeWithTag(interpretationTag).getBoundsInRoot()

            bodyBounds.top shouldBe headerBounds.bottom
            // And the interpretation hugs the bottom of the body (no bottom-pin gap)
            interpretationBounds.top shouldBe bodyBounds.bottom
            // And no scrollbar is shown
            onNodeWithContentDescription(CASE_VIEW_SCROLL_BAR).assertDoesNotExist()
        }
    }

    @Test
    fun `should bound the body and show a scrollbar when content overflows`() {
        // Given a body that is taller than the panel can fit
        val overflowingBodyHeight = 1000
        composeTestRule.setContent {
            Box(modifier = Modifier.size(panelWidth, panelHeight)) {
                CaseInspectionLayout(
                    caseHeader = { headerSlot() },
                    caseBody = { bodySlot(heightDp = overflowingBodyHeight) },
                    interpretationContent = { interpretationSlot() }
                )
            }
        }

        with(composeTestRule) {
            // When the layout decides scrolling is required
            waitUntilExactlyOneExists(hasContentDescription(CASE_VIEW_SCROLL_BAR))

            // Then a scrollbar is rendered
            onNodeWithContentDescription(CASE_VIEW_SCROLL_BAR).assertIsDisplayed()

            // And the body slot is bounded so the interpretation remains visible
            val interpretationBounds = onNodeWithTag(interpretationTag).getBoundsInRoot()
            val headerBounds = onNodeWithTag(headerTag).getBoundsInRoot()
            // Header still at the top
            headerBounds.top shouldBe 0.dp
            // Interpretation pinned at the bottom of the panel
            interpretationBounds.bottom shouldBe panelHeight
        }
    }

    @Test
    fun `should keep the header pinned at the top regardless of body size`() {
        // Given two layouts, one with a small body and one with an overflowing body
        composeTestRule.setContent {
            Box(modifier = Modifier.size(panelWidth, panelHeight)) {
                CaseInspectionLayout(
                    caseHeader = { headerSlot() },
                    caseBody = { bodySlot(heightDp = 1000) },
                    interpretationContent = { interpretationSlot() }
                )
            }
        }

        // When either is measured
        with(composeTestRule) {
            waitUntilExactlyOneExists(hasContentDescription(CASE_VIEW_SCROLL_BAR))

            // Then the header is unaffected by the internal body scroll
            val headerBounds = onNodeWithTag(headerTag).getBoundsInRoot()
            headerBounds.top shouldBe 0.dp
            (headerBounds.bottom - headerBounds.top) shouldBe headerHeight
        }
    }

    @Test
    fun `should fill the panel height when content is smaller than the panel`() {
        // Given a small body that does not fill the panel
        composeTestRule.setContent {
            Box(modifier = Modifier.size(panelWidth, panelHeight)) {
                CaseInspectionLayout(
                    modifier = Modifier.fillMaxWidth().height(panelHeight),
                    caseHeader = { headerSlot() },
                    caseBody = { bodySlot(heightDp = 40) },
                    interpretationContent = { interpretationSlot() }
                )
            }
        }

        // When the layout reports its size
        with(composeTestRule) {
            // Then header is at the top, interpretation hugs the body, and any
            // unused space falls below the interpretation rather than recentring
            // the children.
            val headerBounds = onNodeWithTag(headerTag).getBoundsInRoot()
            val bodyBounds = onNodeWithTag(bodyTag).getBoundsInRoot()
            val interpretationBounds = onNodeWithTag(interpretationTag).getBoundsInRoot()

            headerBounds.top shouldBe 0.dp
            bodyBounds.top shouldBe headerBounds.bottom
            interpretationBounds.top shouldBe bodyBounds.bottom
        }
    }

    @Composable
    private fun headerSlot() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .testTag(headerTag)
        )
    }

    @Composable
    private fun bodySlot(heightDp: Int) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .testTag(bodyTag)
        )
    }

    @Composable
    private fun interpretationSlot() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(interpretationHeight)
                .testTag(interpretationTag)
        )
    }
}

