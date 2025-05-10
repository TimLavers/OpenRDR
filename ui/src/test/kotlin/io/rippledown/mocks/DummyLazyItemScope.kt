package io.rippledown.mocks

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

class DummyLazyItemScope: LazyItemScope {
    private val modifier = DummyModifier()

    override fun Modifier.animateItem(
        fadeInSpec: FiniteAnimationSpec<Float>?,
        placementSpec: FiniteAnimationSpec<IntOffset>?,
        fadeOutSpec: FiniteAnimationSpec<Float>?
    ) = modifier

    override fun Modifier.fillParentMaxHeight(fraction: Float) = modifier

    override fun Modifier.fillParentMaxSize(fraction: Float) = modifier

    override fun Modifier.fillParentMaxWidth(fraction: Float) = modifier
}