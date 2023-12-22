package io.rippledown.proxy

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.rippledown.constants.caseview.CASES
import io.rippledown.constants.caseview.NUMBER_OF_CASES_ID

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

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.requireNumberOfCases(expected: Int) {
    waitUntilExactlyOneExists(hasText("$CASES $expected"), timeoutMillis = 2_000)
}


//TODO: remove this
suspend fun <T> act(
    block: () -> T,
): T {
    return block()
}
