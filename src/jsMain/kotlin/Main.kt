import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import react.create
import react.dom.render

fun main() {
    document.getElementById("root")?.let { container ->
        val ui = OpenRDRUI.create {
            api = Api()
            scope = MainScope()
        }
        render(ui, container)
    }
}


