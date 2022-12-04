import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import react.create
import react.dom.client.createRoot

fun main() {
    document.getElementById("root")?.let { container ->
        val ui = OpenRDRUI.create {
            api = Api()
            scope = MainScope()
        }
        createRoot(container.unsafeCast<dom.Element>()).render(ui)
    }
}


