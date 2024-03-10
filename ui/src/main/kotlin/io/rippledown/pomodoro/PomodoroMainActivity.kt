package io.rippledown.pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.rippledown.constants.main.TITLE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        icon = painterResource("water-wave-icon.png"),
        title = TITLE
    ) {
                // Create the list of items
                val tasks =
                        mutableListOf(
                            PomodoroTask("Apples", "Green", 0.5F, 2432, "2/3/24"),
                            PomodoroTask("Bananas", "Yellow", 0.2F, 100, "3/3/24"),
                            PomodoroTask("Oranges", "Orange", 0.9F, 0, "3/3/24"),
                            PomodoroTask("Pears", "Green", 0.1F, 0, "4/3/24")
                        )


                fun swap(a: Int, b: Int) {
                    println("swap $a, $b")
                    tasks.swap(a,b)
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    PomodoroListScreen(tasks,
                        { a: Int, b: Int -> swap(a, b)},
                        Modifier.fillMaxSize()
                    )
                }
            }
        }

