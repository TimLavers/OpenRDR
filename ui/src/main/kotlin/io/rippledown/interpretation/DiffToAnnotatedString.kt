package io.rippledown.interpretation

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import io.rippledown.constants.interpretation.ADDING
import io.rippledown.constants.interpretation.BY
import io.rippledown.constants.interpretation.REMOVING
import io.rippledown.constants.interpretation.REPLACING
import io.rippledown.decoration.BACKGROUND_COLOR
import io.rippledown.model.diff.Addition
import io.rippledown.model.diff.Diff
import io.rippledown.model.diff.Removal
import io.rippledown.model.diff.Replacement

val styleForKeyWord = SpanStyle(background = BACKGROUND_COLOR)
fun Diff.toAnnotatedString() = when (this) {
    is Addition -> buildAnnotatedString {
        append(ADDING)
        withStyle(style = styleForKeyWord) {
            append(addedText)
        }
    }

    is Replacement -> buildAnnotatedString {
        append(REPLACING)
        withStyle(style = styleForKeyWord) {
            append(originalText)
        }
        append(BY)
        withStyle(style = styleForKeyWord) {
            append(replacementText)
        }
    }

    is Removal -> buildAnnotatedString {
        append(REMOVING)
        withStyle(style = styleForKeyWord) {
            append(removedText)
        }
    }
}