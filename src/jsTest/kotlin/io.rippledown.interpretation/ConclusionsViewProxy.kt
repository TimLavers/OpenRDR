package io.rippledown.interpretation

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import proxy.findById
import react.dom.test.act
import web.dom.Element
import web.dom.NodeList
import web.html.HTMLElement

fun HTMLElement.treeItems(): NodeList<Element> = querySelectorAll("[role='treeitem']")

fun HTMLElement.requireTreeItemCount(expectedCount: Int) {
    treeItems().length shouldBe expectedCount
}

fun HTMLElement.requireTreeItems(vararg expectedTexts: String) {
    val found = treeItems()
    found.length shouldBe expectedTexts.size
    expectedTexts.forEachIndexed { index, text ->
        found[index].textContent shouldContain text
    }
}

suspend fun HTMLElement.clickComment(text: String) {
    val commentComponent = findById("COMMENT:$text")
    act { commentComponent.click() }
}
