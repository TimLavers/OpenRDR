package io.rippledown.proxy

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag

fun findById(id : String) {
    println("findById")
}
@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.findById(id: String) = onNodeWithTag(id)

fun ComposeTestRule.findAllById(id: String) {
     onAllNodesWithTag(id)
    println("findAllById")
}
fun findId(id: String) {
    println("findAllById")
}


