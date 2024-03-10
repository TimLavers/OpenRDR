package io.rippledown.realityexpander.dragdropcolumncompose

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
                val itemsStateFlow =
                    MutableStateFlow(
                        listOf(
                            Item(name ="Item 1 - Apple"),
                            Item(name ="Item 2 - Banana"),
                            Item(name ="Item 3 - Carrot"),
                            Item(name ="Item 4 - Date"),
                            Item(name ="Item 5 - Eggplant"),
                            Item(name ="Item 6 - Fig"),
                            Item(name ="Item 7 - Grape"),
                            Item(name ="Item 8 - Honeydew"),
                            Item(name ="Item 9 - Iceberg Lettuce"),
                        )
                    )

                // Define what happens when an item is clicked
                fun onItemClicked(clickedItem: Item) {
                    itemsStateFlow.update { currentList ->
                        val newList = currentList.toMutableList()
                            .map { item ->
                                if(clickedItem == item) {
                                    // Could perform some other action here...
                                    item.copy(name = "Clicked ${item.name}")
                                } else {
                                    item
                                }
                            }
                            .toList()

                        newList
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    DragNDropItemsList(
                        itemsStateFlow.collectAsState().value,
                        onItemClicked = ::onItemClicked
                    )
                }
            }
        }

data class Item(
    val id: Int = internalId++,
    val name: String = "",
    val description: String = "",
    val color: Long = Random(id).nextLong()
) {
    companion object {
        private var internalId = 0  // <-- OK to use in examples, never in production!
    }
}

@Composable
fun DragNDropItemsList(
    items: List<Item>,
    onItemClicked : (Item) -> Unit = {}
) {
    val mutableUiState =
        MutableStateFlow<List<Item>>(items)
    val uiState = mutableUiState.asStateFlow().collectAsState()

    fun swapItems(from: Int, to: Int) {
        val fromItem = mutableUiState.value[from]
        val toItem = mutableUiState.value[to]
        val newList = mutableUiState.value.toMutableList()
        newList[from] = toItem
        newList[to] = fromItem

        mutableUiState.update { newList }
    }

    LaunchedEffect(key1 = Unit) {
        mutableUiState.update { items.shuffled() }
    }

    DragDropColumn(
        items = uiState.value,
        onSwap = ::swapItems
    ) { item ->
        Card(
            modifier = Modifier
                .clickable {
                    onItemClicked(item)
                },
        ) {
            Text(
                text = item.name,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(item.color))
                    .padding(16.dp),
            )
        }
    }
}