package io.rippledown.utils

import androidx.compose.ui.test.junit4.ComposeTestRule

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
