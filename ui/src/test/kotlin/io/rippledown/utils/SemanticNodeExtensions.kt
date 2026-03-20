package io.rippledown.utils

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.printToString

fun SemanticsNodeInteraction.dump() {
    println("\n" + printToString())
}

fun ComposeTestRule.waitUntilAsserted(timeoutMillis: Long = 1_000, block: () -> Unit) {
    waitUntil(timeoutMillis) {
        try {
            block()
            true
        } catch (e: Error) {
            false
        }
    }
}