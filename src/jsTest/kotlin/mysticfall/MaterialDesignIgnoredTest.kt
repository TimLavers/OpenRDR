package mysticfall

import io.kotest.matchers.shouldBe
import js.core.jso
import mui.base.TextareaAutosize
import mui.lab.TreeItem
import mui.lab.TreeView
import mui.material.TextField
import mui.material.TextFieldProps
import proxy.findById
import proxy.printJSON
import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p
import kotlin.test.Test
import kotlin.test.assertEquals

//These tests all fail
@Ignore
class MaterialDesignIgnoredTest : ReactTestSupport {

    @Test
    fun shouldRenderMuiTreeView() {
        val ComponentWithTreeView = FC<Props> {
            TreeView {

                TreeItem {
                    nodeId = "id_1"
                    label = createElement(p, jso {
                        title = "label 1"
                    }
                    )

                }
            }
        }
        val renderer = render {
            ComponentWithTreeView {
            }
        }
        renderer.printJSON()
        val instances = renderer.root.findAllByType(TreeItem)

        instances.map {
            it.props.nodeId
        } shouldBe listOf("id_1")

    }

    class ComponentWithTextField : Component<Props, State>() {
        override fun render(): ReactNode? {
            println("render")
            val textFieldProps = jso<TextFieldProps> {
                id = "text field id"
                defaultValue = "Some text to show"
                multiline = true
            }
            return createElement(TextField, textFieldProps)
        }

        override fun componentDidCatch(error: Throwable, info: ErrorInfo) {
            println("error: $error")
            println("info: $info")
        }
    }

    @Test
    fun shouldRenderMuiMultilineTextFieldClass() {
        val renderer = render {
            div {
                child(
                    ComponentWithTextField().render()
                )
            }
        }
        val found = renderer.root.findByType(TextField)
        assertEquals(found.props.defaultValue, "Some text to show")
    }

    @Test
    fun shouldRenderMuiTextareaAutosize() {
        val ComponentWithTextareaAutosize = FC<TestProps> { props ->
            div {
                TextareaAutosize {
                    id = "id"
                    defaultValue = props.name
                }
            }
        }
        val renderer = render {
            div {
                ComponentWithTextareaAutosize {
                    name = "Some text to show"
                }
            }
        }
        val found = renderer.findById("id")
        found.props.asDynamic().value.unsafeCast<String>() shouldBe "Some text to show"
    }


}
