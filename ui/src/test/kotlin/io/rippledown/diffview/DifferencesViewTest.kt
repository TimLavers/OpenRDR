package io.rippledown.diffview

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.mockk.mockk
import io.rippledown.interpretation.DifferencesView
import io.rippledown.interpretation.DifferencesViewHandler
import io.rippledown.model.diff.*

class DifferencesViewTest {
}

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            val handler = mockk<DifferencesViewHandler>(relaxed = true)
            val diffList = DiffList(
                listOf(
                    Unchanged("Weather is fine."),
                    Addition("Go to Bondi Beach."),
                    Addition("And bring flippers."),
                    Replacement("Surf is good.", "Surf is great."),
                    Removal("Don't stay long.")
                )
            )
            DifferencesView(diffList, handler)
        }
    }
}
