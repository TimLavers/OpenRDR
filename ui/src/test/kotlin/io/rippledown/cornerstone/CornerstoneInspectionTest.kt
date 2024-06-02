package io.rippledown.cornerstone

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.rippledown.constants.cornerstone.CORNERSTONE_CASE_NAME_ID
import io.rippledown.constants.cornerstone.CORNERSTONE_ID
import io.rippledown.constants.cornerstone.CORNERSTONE_TITLE
import io.rippledown.constants.rule.FINISH_RULE_BUTTON
import io.rippledown.interpretation.requireNoDifferencesTab
import io.rippledown.model.createCase
import io.rippledown.model.rule.CornerstoneStatus
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

class CornerstoneInspectionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    val name = "Greta"
    val case = createCase(name)
    val cornerstoneStatus = CornerstoneStatus(case, 42, 100)



    @Test
    fun `should display a label for the cornerstone case`() {
        with(composeTestRule) {
            setContent {
                CornerstoneInspection(case)
            }
            onNodeWithContentDescription(CORNERSTONE_ID)
                .assertTextEquals(CORNERSTONE_TITLE)
        }
    }

    @Test
    fun `should display the name of the cornerstone case`() {
        with(composeTestRule) {
            setContent {
                CornerstoneInspection(case)
            }
            onNodeWithContentDescription(CORNERSTONE_CASE_NAME_ID)
                .assertTextEquals(name)
        }

    }

    @Test
    fun `should not show the differences tab`() {
        with(composeTestRule) {
            setContent {
                CornerstoneInspection(case)
            }
            requireNoDifferencesTab()
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CasePager() {

    Box {
        val pagerState = rememberPagerState(
            initialPage = 2,
            pageCount = {
                10
            })
        VerticalPager(state = pagerState) { page ->
            // Our page content
            val case = createCase("Greta " + page)
            val conerstoneStatus = CornerstoneStatus(case, 42, 100)
            CornerstoneInspection(case)
        }
        val coroutineScope = rememberCoroutineScope()
        Button(
            onClick = {
                coroutineScope.launch {
                    // Call scroll to on pagerState
                    pagerState.animateScrollToPage(5)
                }
            },
            modifier = androidx.compose.ui.Modifier.semantics { contentDescription = FINISH_RULE_BUTTON }
                .align(Alignment.BottomEnd)

        )
        {
            Text("Jump to Page 5")
        }
    }
}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            CornerstoneInspection(createCase("Greta"))
        }
    }
}


