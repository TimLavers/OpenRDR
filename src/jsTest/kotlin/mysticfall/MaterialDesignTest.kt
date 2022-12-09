package mysticfall

import io.kotest.matchers.shouldBe
import mui.base.TextareaAutosize
import mui.material.Button
import mui.material.TextField
import mui.material.Typography
import mui.material.styles.TypographyVariant
import proxy.findById
import react.FC
import react.dom.html.ReactHTML.div
import kotlin.test.Test

class MaterialDesignTest : ReactTestSupport {

    @Test
    fun findAndClickMuiButtonComponent() {
        var clicked = false
        val ButtonComponent = FC<TestProps> { props ->
            div {
                Button {
                    id = "button id"
                    +props.name
                    onClick = { clicked = true }
                }
            }
        }
        val renderer = render {
            div {
                ButtonComponent {
                    name = "Click me"
                }
            }
        }
        val found = renderer.findById("button id")
        found.props.asDynamic().onClick()
        clicked shouldBe true
    }

    @Test
    fun findTextOfMuiTypographyComponent() {
        val TypographyComponent = FC<TestProps> { props ->
            div {
                Typography {
                    variant = TypographyVariant.h1
                    +props.name
                }
            }
        }
        val renderer = render {
            div {
                TypographyComponent {
                    name = "Typo"
                }
            }
        }
        val found = renderer.root.findByType(TypographyComponent)
        found.props.name shouldBe "Typo"
    }

    @Test
    fun shouldRenderMuiSinglelineTextField() {
        val ComponentWithTextField = FC<TestProps> { props ->
            div {
                TextField {
                    id = "text field id"
                    value = props.name
                }
            }
        }
        val renderer = render {
            div {
                ComponentWithTextField {
                    name = "Some text to show"
                }
            }
        }
        val found = renderer.findById("text field id")
        found.props.asDynamic().value.unsafeCast<String>() shouldBe "Some text to show"
    }

    @Test
    fun shouldRenderMuiMultilineTextField() {
        val ComponentWithTextField = FC<TestProps> { props ->
            div {
                TextField {
                    id = "text field id"
                    defaultValue = props.name
                    multiline = true
                }
            }
        }
        val renderer = render {
            div {
                ComponentWithTextField {
                    name = "Some text to show"
                }
            }
        }
        val found = renderer.findById("text field id")
        found.props.asDynamic().value.unsafeCast<String>() shouldBe "Some text to show"
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
