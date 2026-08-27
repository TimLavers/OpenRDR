package io.rippledown.casecontrol

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.constants.caseview.CASE_NAME_PREFIX
import io.rippledown.constants.caseview.CORNERSTONE_SECTION_HEADER_ID
import io.rippledown.constants.caseview.FAVOURITES_SECTION_HEADER_ID
import io.rippledown.constants.caseview.PROCESSED_SECTION_HEADER_ID
import io.rippledown.model.CaseId
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

/**
 * The vertical arrangement of the case list's sections: each section takes only
 * the height its cases need, so that a section header always sits directly below
 * the section above it, whatever is expanded. Giving each expanded section a
 * weight instead shares the panel's height between them, which drops the headers
 * of the later sections down the panel — and to its very bottom when the sections
 * below them are collapsed.
 */
class CaseSelectorLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var handler: CaseSelectorHandler

    private val processed = listOf(CaseId(id = 1, name = "p1"))
    private val cornerstones = listOf(CaseId(id = 2, name = "cs1"))
    private val favourites = listOf(CaseId(id = 3, name = "f1"))

    /** A header's own vertical padding, the most that may separate sections. */
    private val maximumGap = 16.dp

    @Before
    fun setUp() {
        handler = mockk<CaseSelectorHandler>(relaxed = true)
    }

    private fun caseDescription(name: String) = "$CASE_NAME_PREFIX$name"

    private fun showAllThreeSections() = composeTestRule.setContent {
        CaseSelector(processed, cornerstones, handler, favourites)
    }

    private fun topOf(description: String): Dp =
        composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed().getBoundsInRoot().top

    private fun bottomOf(description: String): Dp =
        composeTestRule.onNodeWithContentDescription(description).assertIsDisplayed().getBoundsInRoot().bottom

    private infix fun Dp.shouldSitDirectlyBelow(above: Dp) {
        (this - above) shouldBeGreaterThanOrEqualTo 0.dp
        (this - above) shouldBeLessThan maximumGap
    }

    @Test
    fun `each section header sits directly below the section above it`() = runTest {
        // Given all three sections, each with a case, all expanded
        showAllThreeSections()

        // Then each header follows the last case of the section above it, rather
        // than being spaced out down the panel
        with(composeTestRule) {
            topOf(CORNERSTONE_SECTION_HEADER_ID) shouldSitDirectlyBelow bottomOf(caseDescription("p1"))
            topOf(FAVOURITES_SECTION_HEADER_ID) shouldSitDirectlyBelow bottomOf(caseDescription("cs1"))
        }
    }

    @Test
    fun `the first section starts at the top of the panel`() = runTest {
        showAllThreeSections()

        topOf(PROCESSED_SECTION_HEADER_ID) shouldBe 0.dp
    }

    @Test
    fun `collapsing a section leaves the headers where they were`() = runTest {
        // Given all three sections expanded
        showAllThreeSections()
        val cornerstoneHeaderTop = topOf(CORNERSTONE_SECTION_HEADER_ID)
        val favouritesHeaderTop = topOf(FAVOURITES_SECTION_HEADER_ID)

        // When the last section is collapsed
        with(composeTestRule) {
            onNodeWithContentDescription(FAVOURITES_SECTION_HEADER_ID).performClick()
            waitForIdle()
        }

        // Then the headers above its cases have not moved: only the cases it was
        // showing have gone
        topOf(CORNERSTONE_SECTION_HEADER_ID) shouldBe cornerstoneHeaderTop
        topOf(FAVOURITES_SECTION_HEADER_ID) shouldBe favouritesHeaderTop
    }

    @Test
    fun `a collapsed section leaves its header directly below the section above it`() = runTest {
        // Given all three sections expanded
        showAllThreeSections()

        // When the middle section is collapsed
        with(composeTestRule) {
            onNodeWithContentDescription(CORNERSTONE_SECTION_HEADER_ID).performClick()
            waitForIdle()
        }

        // Then the section below it moves up to meet it, rather than staying put
        // or dropping to the bottom of the panel
        with(composeTestRule) {
            topOf(FAVOURITES_SECTION_HEADER_ID) shouldSitDirectlyBelow bottomOf(CORNERSTONE_SECTION_HEADER_ID)
        }
    }

    @Test
    fun `collapsing every section stacks the headers at the top of the panel`() = runTest {
        // Given all three sections expanded
        showAllThreeSections()

        // When all of them are collapsed
        with(composeTestRule) {
            onNodeWithContentDescription(PROCESSED_SECTION_HEADER_ID).performClick()
            onNodeWithContentDescription(CORNERSTONE_SECTION_HEADER_ID).performClick()
            onNodeWithContentDescription(FAVOURITES_SECTION_HEADER_ID).performClick()
            waitForIdle()
        }

        // Then the three headers are stacked, none of them pushed to the bottom
        with(composeTestRule) {
            topOf(CORNERSTONE_SECTION_HEADER_ID) shouldSitDirectlyBelow bottomOf(PROCESSED_SECTION_HEADER_ID)
            topOf(FAVOURITES_SECTION_HEADER_ID) shouldSitDirectlyBelow bottomOf(CORNERSTONE_SECTION_HEADER_ID)
        }
    }
}
