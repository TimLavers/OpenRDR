import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import react.create
import react.dom.client.createRoot
import web.dom.Element

fun main() {
    document.getElementById("root")?.let { container ->
        val ui = OpenRDRUI.create {
            api = Api()
            scope = MainScope()
        }
        createRoot(container.unsafeCast<Element>()).render(ui)
    }
}


