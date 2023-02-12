package mysticfall

import js.core.globalThis
import react.VFC
import react.dom.test.createRoot
import react.dom.test.unmount
import web.dom.document
import web.html.HTML.div
import web.html.HTMLElement

suspend fun checkContainer(vfc: VFC, block: (container: HTMLElement) -> Unit) {
    globalThis.IS_REACT_ACT_ENVIRONMENT = true
    val container = document.createElement(div)
    document.body.appendChild(container)
    val root = createRoot(container, vfc)
    block(container)
    unmount(root)
}


