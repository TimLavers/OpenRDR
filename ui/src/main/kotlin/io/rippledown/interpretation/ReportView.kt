package io.rippledown.interpretation

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownPadding
import io.rippledown.chat.TypingIndicator
import io.rippledown.constants.interpretation.*
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.getTextInNode

internal val powerOfTenRegex = Regex("""10\^(-?\d+)""")
private val microPrefixRegex = Regex("""\bu(?=[a-zA-Z])""")

/**
 * Pre-process report text to render powers of 10 as superscripts and
 * the micro prefix as Greek mu, matching the case view's UnitsCell behavior
 * (e.g. 10^12 -> 10¹², umol/L -> μmol/L).
 * Note. We can't use UnitsCell's implementation here because AnnotatedString is not available in Markdown
 */
internal fun formatReportText(text: String): String {
    val withMu = text.replace(microPrefixRegex, "μ")
    return withMu.replace(powerOfTenRegex) { match ->
        val exponent = match.groupValues[1]
        // Use Unicode superscript digits for the exponent
        val superscript = exponent.map { char ->
            when (char) {
                '-' -> '⁻'
                '0' -> '⁰'
                '1' -> '¹'
                '2' -> '²'
                '3' -> '³'
                '4' -> '⁴'
                '5' -> '⁵'
                '6' -> '⁶'
                '7' -> '⁷'
                '8' -> '⁸'
                '9' -> '⁹'
                else -> char
            }
        }.joinToString("")
        "10$superscript"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReportView(
    reportText: String?,   // null = not yet loaded / loading
    isVisible: Boolean,
    onToggle: (Boolean) -> Unit,
    isLoading: Boolean = false,
    hasComments: Boolean = true, // false when the case has no comments to report on
) {
    var justCopied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val copyInteractionSource = remember { MutableInteractionSource() }
    val isCopyHovered by copyInteractionSource.collectIsHoveredAsState()

    if (justCopied) {
        LaunchedEffect(justCopied) {
            kotlinx.coroutines.delay(COPY_CONFIRMATION_MS)
            justCopied = false
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .clickable { onToggle(!isVisible) }
                .semantics { contentDescription = REPORT_TOGGLE },
        ) {
            Icon(
                imageVector = if (isVisible) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Report",
                color = Color.DarkGray,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            TooltipArea(
                tooltip = {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = REPORT_DISCLAIMER,
                            style = TextStyle(fontSize = 12.sp),
                            modifier = Modifier.widthIn(max = 400.dp).padding(8.dp)
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = Color.DarkGray.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(14.dp)
                        .semantics { contentDescription = REPORT_DISCLAIMER_ICON }
                )
            }
        }
        if (isVisible) {
            // Match the 4.dp gap the case-list section headers leave between
            // the header and their list, for a consistent look.
            Spacer(modifier = Modifier.height(4.dp))
            if (isLoading) {
                // While generating, show the typing indicator between the
                // "Report" label and the panel (rather than inside the card),
                // so it reads like the chat's typing indicator does.
                TypingIndicator()
            } else OutlinedCard(
                modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                    .semantics { contentDescription = REPORT_PANEL },
                colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .hoverable(copyInteractionSource)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                            .padding(end = 28.dp, start = 8.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        when {
                            !hasComments -> Text("No comments to report on.")
                            reportText.isNullOrBlank() -> Text("No report.")
                            else -> {
                                val formattedText = formatReportText(reportText)
                                Box {
                                    // Zero-size hidden text for accessibility - the markdown
                                    // renderer doesn't expose its content to the Java
                                    // accessibility bridge, so tests read this instead. It is
                                    // clipped to zero size so it adds no visible layout space.
                                    Text(
                                        text = formattedText,
                                        modifier = Modifier
                                            .size(0.dp)
                                            .semantics { contentDescription = REPORT_TEXT }
                                    )
                                    Markdown(
                                        content = formattedText,
                                        typography = markdownTypography(
                                            h2 = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                            h3 = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                            h4 = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                            h5 = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                            h6 = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                            paragraph = TextStyle(fontSize = 12.sp),
                                            list = TextStyle(fontSize = 12.sp),
                                            bullet = TextStyle(fontSize = 12.sp)
                                        ),
                                        // The renderer's default 4.dp above and below every
                                        // list item leaves 8.dp between consecutive bullets,
                                        // which reads as separate paragraphs rather than one
                                        // list. 1.dp each keeps the items grouped while still
                                        // separating them.
                                        padding = markdownPadding(
                                            listItemTop = 1.dp,
                                            listItemBottom = 1.dp
                                        ),
                                        // Render bold (out-of-range) values in the same red used
                                        // by the case view, so the flagged-value cue is consistent
                                        // across both panels. The default renderer only makes bold
                                        // text heavier, with no colour, so we intercept STRONG nodes.
                                        annotator = markdownAnnotator { content, child ->
                                            if (child.type == MarkdownElementTypes.STRONG) {
                                                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.Red))
                                                // Append only the content, skipping the "**"/"__"
                                                // emphasis delimiter tokens that wrap it.
                                                child.children
                                                    .filter { it.type != MarkdownTokenTypes.EMPH }
                                                    .forEach { node ->
                                                        append(node.getTextInNode(content).toString())
                                                    }
                                                pop()
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    // Copy button is placed AFTER the content so it renders on top
                    // of the scrollable Column and stays clickable (matching
                    // AiReportSectionView). If placed before, the Column's
                    // verticalScroll intercepts the pointer events.
                    if (!reportText.isNullOrBlank() && !isLoading && hasComments) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(formatReportText(reportText)))
                                justCopied = true
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .alpha(if (justCopied || isCopyHovered) 1f else COPY_RESTING_ALPHA)
                                .semantics { contentDescription = REPORT_COPY_ICON }
                        ) {
                            Icon(
                                imageVector = if (justCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = if (justCopied) Color(0xFF2E7D32) else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Resting opacity of the copy icon, so it is discoverable without hovering. */
private const val COPY_RESTING_ALPHA = 0.35f

/** How long the copy button shows its "copied" check before reverting. */
private const val COPY_CONFIRMATION_MS = 1500L
