package io.rippledown.mocks

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Measured

class DummyRowScope: RowScope {
    private val modifier = DummyModifier()
    override fun Modifier.align(alignment: Alignment.Vertical): Modifier {
        return modifier
    }

    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier {
        return modifier
    }

    override fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine): Modifier {
        return modifier
    }

    override fun Modifier.alignByBaseline(): Modifier {
        return modifier
    }

    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier {
        return modifier
    }
}