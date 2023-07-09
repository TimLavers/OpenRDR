 package react.dom

import js.core.globalThis
import react.FC
import react.Props
import react.dom.test.act
import react.dom.test.createRoot
import react.dom.test.unmount
import web.dom.document
import web.html.HTML.div
import web.html.HTMLElement

 suspend fun checkContainer(fc: FC<Props>, block: (container: HTMLElement) -> Unit) {
     globalThis.IS_REACT_ACT_ENVIRONMENT = true
     val container = document.createElement(div)
     document.body.appendChild(container)
     val root = createRoot(container, fc)
     act {
         block(container)
     }
     unmount(root)
 }

 suspend fun createRootFor(fc: FC<Props>): HTMLElement {
     globalThis.IS_REACT_ACT_ENVIRONMENT = true
     val container = document.createElement(div)
     document.body.appendChild(container)
     createRoot(container, fc)
     return container
 }

