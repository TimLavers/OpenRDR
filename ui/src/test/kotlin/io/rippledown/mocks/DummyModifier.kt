package io.rippledown.mocks

import androidx.compose.ui.Modifier

class DummyModifier: Modifier {
    override fun all(predicate: (Modifier.Element) -> Boolean): Boolean {
        return true
    }

    override fun any(predicate: (Modifier.Element) -> Boolean): Boolean {
        return true
    }

    override fun <R> foldIn(initial: R, operation: (R, Modifier.Element) -> R): R {
        return initial
    }

    override fun <R> foldOut(initial: R, operation: (Modifier.Element, R) -> R): R {
        return initial
    }
}