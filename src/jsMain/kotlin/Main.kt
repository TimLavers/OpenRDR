import kotlinx.coroutines.MainScope
import react.create
import react.dom.client.createRoot
import web.dom.Element
import web.dom.document

fun main() {
    document.getElementById("root")?.let { container ->
        val ui = OpenRDRUI.create {
            api = Api()
            scope = MainScope()
        }
        createRoot(container.unsafeCast<Element>()).render(ui)
    }
}