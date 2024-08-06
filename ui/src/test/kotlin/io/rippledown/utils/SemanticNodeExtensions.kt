package io.rippledown.utils

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.printToString

fun SemanticsNodeInteraction.dump() {
    println("\n" + printToString())
}