package io.rippledown.mocks

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.unit.IntOffset

class DummyLazyItemScope: LazyItemScope {
    private val modifier = DummyModifier()

    @ExperimentalFoundationApi
    override fun Modifier.animateItemPlacement(animationSpec: FiniteAnimationSpec<IntOffset>): Modifier {
        return modifier
    }

    override fun Modifier.fillParentMaxHeight(fraction: Float): Modifier {
        return modifier
    }

    override fun Modifier.fillParentMaxSize(fraction: Float): Modifier {
        return modifier
    }

    override fun Modifier.fillParentMaxWidth(fraction: Float): Modifier {
        return modifier
    }
}