package io.rippledown.casecontrol

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.rippledown.constants.caseview.CASE_VIEW_SCROLL_BAR

/**
 * Lays out a fixed [caseHeader] (e.g. case name + dates), a [caseBody] of
 * attribute rows, and a fixed [interpretationContent] in a vertical stack:
 *
 *  - Header stays at the top of the panel and never scrolls.
 *  - Interpretation stays at the bottom (pinned beneath the body) and never
 *    scrolls.
 *  - Body wraps to its natural height when small (interpretation hugs it from
 *    below), or fills the remaining height with an internal vertical scroll
 *    bar when it would otherwise overflow.
 *
 * The wrap-or-scroll decision is made via a two-pass `SubcomposeLayout`:
 * the body is first measured with unbounded height to learn its natural
 * height. If that fits in the slot left by the header and interpretation, the
 * body is rendered without scrolling; otherwise it is re-subcomposed inside a
 * `verticalScroll` and bounded to the available height. A plain
 * `Modifier.verticalScroll` cannot replace this logic because it always
 * reports its assigned slot height, which would push the interpretation to
 * the bottom of the panel even for tiny cases.
 */
@Composable
fun CaseInspectionLayout(
    modifier: Modifier = Modifier,
    caseHeader: @Composable () -> Unit,
    caseBody: @Composable () -> Unit,
    interpretationContent: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    val scrollbarAdapter = rememberScrollbarAdapter(scrollState)
    SubcomposeLayout(modifier = modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val maxWidth = constraints.maxWidth

        val headerPlaceables = subcompose(SlotId.Header) { caseHeader() }
            .map { it.measure(loose) }
        val headerHeight = headerPlaceables.maxOfOrNull { it.height } ?: 0
        val headerWidth = headerPlaceables.maxOfOrNull { it.width } ?: 0

        val interpretationPlaceables = subcompose(SlotId.Interpretation) { interpretationContent() }
            .map { it.measure(loose) }
        val interpretationHeight = interpretationPlaceables.maxOfOrNull { it.height } ?: 0
        val interpretationWidth = interpretationPlaceables.maxOfOrNull { it.width } ?: 0

        val availableForBody =
            (constraints.maxHeight - headerHeight - interpretationHeight).coerceAtLeast(0)

        val probePlaceables = subcompose(SlotId.BodyProbe) { caseBody() }
            .map {
                it.measure(
                    Constraints(
                        minWidth = 0,
                        maxWidth = maxWidth,
                        minHeight = 0,
                        maxHeight = Constraints.Infinity
                    )
                )
            }
        val probeHeight = probePlaceables.maxOfOrNull { it.height } ?: 0
        val probeWidth = probePlaceables.maxOfOrNull { it.width } ?: 0

        val needsScrolling = probeHeight > availableForBody
        val bodyPlaceables: List<Placeable>
        val bodyHeight: Int
        val bodyWidth: Int
        if (!needsScrolling) {
            bodyPlaceables = probePlaceables
            bodyHeight = probeHeight
            bodyWidth = probeWidth
        } else {
            bodyPlaceables = subcompose(SlotId.BodyScrolling) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    ) {
                        caseBody()
                    }
                    VerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(8.dp)
                            .semantics { contentDescription = CASE_VIEW_SCROLL_BAR },
                        adapter = scrollbarAdapter
                    )
                }
            }.map {
                it.measure(
                    loose.copy(maxWidth = maxWidth, maxHeight = availableForBody)
                )
            }
            bodyHeight = bodyPlaceables.maxOfOrNull { it.height } ?: 0
            bodyWidth = bodyPlaceables.maxOfOrNull { it.width } ?: 0
        }

        val contentHeight = headerHeight + bodyHeight + interpretationHeight
        val width = maxOf(headerWidth, bodyWidth, interpretationWidth)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        // Fill the assigned height when the parent demands it (e.g.
        // `Modifier.fillMaxHeight()`), so the layout occupies the whole panel
        // and the header/body/interpretation render at their actual y offsets
        // rather than being recentred. Any unused height ends up below the
        // interpretation when the case is small.
        val height = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            headerPlaceables.forEach { it.place(0, 0) }
            bodyPlaceables.forEach { it.place(0, headerHeight) }
            interpretationPlaceables.forEach { it.place(0, headerHeight + bodyHeight) }
        }
    }
}

private enum class SlotId { Header, BodyProbe, BodyScrolling, Interpretation }
