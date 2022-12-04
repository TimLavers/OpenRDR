package mysticfall

import csstype.ClassName
import org.w3c.dom.Element
import react.*
import react.dom.html.ReactHTML.div
import kotlin.js.JSON.stringify
import kotlin.js.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TestRendererTest : ReactTestSupport {

    fun createWithAct(builder: ChildrenBuilder.() -> Unit): TestRenderer {
        lateinit var result: TestRenderer
        act {
            result = render {
                builder()
            }
        }
        return result
    }

    private fun withFunctionComponent(block: (ComponentType<TestProps>, TestRenderer) -> Unit) {
        val renderer = createWithAct {
            TestFuncComponent {
                name = "Function Test"
            }
        }
        block(TestFuncComponent, renderer)
    }

    @Test
    fun testRootWithFunctionComponent() {
        val block = { type: ComponentType<TestProps>, renderer: TestRenderer -> checkType(type, renderer) }
        withFunctionComponent(block)
    }

    private fun checkType(type: ComponentType<TestProps>, renderer: TestRenderer) {
        assertEquals(type.toString(), renderer.root.type.toString(), "Unexpected root type for $type")
    }

    @Test
    fun testFunctionToJSON() = withFunctionComponent { type, renderer ->
        val expected = """
          |{
          |  "type": "div",
          |  "props": {
          |     "className": "test-component"
          |  },
          |  "children": [
          |     {
          |         "type": "h1",
          |         "props": {
          |             "className": "title"
          |         },
          |         "children": [
          |             "First: Function Test"
          |         ]
          |     }
          |  ]
          |}
        """.trimMargin()

        assertEquals(
            stringify(JSON.parse(expected)),
            stringify(renderer.toJSON()),
            "Unexpected JSON contents for $type"
        )
    }

    @Test
    fun testToTree() = withFunctionComponent { type, component ->
        val actual = component.toTree().asDynamic()
        val message = "Unexpected return value of toTree() for $type"
        assertEquals("component", actual["nodeType"], message)
        assertEquals("component", actual["rendered"]["nodeType"], message)
        assertEquals("div", actual["rendered"]["rendered"]["type"], message)
        assertEquals("test-component", actual["rendered"]["rendered"]["props"]["className"], message)
        assertEquals("h1", actual["rendered"]["rendered"]["rendered"][0]["type"], message)
        assertEquals("title", actual["rendered"]["rendered"]["rendered"][0]["props"]["className"], message)
        assertEquals("First: Function Test", actual["rendered"]["rendered"]["rendered"][0]["rendered"][0], message)
    }

    @Test
    fun testUpdate() = withFunctionComponent { type, renderer ->
        act {
            update(renderer) {
                div {
                    className = ClassName("test")
                }
            }
        }

        val actual = stringify(renderer.toJSON())
        val expected = """{"type":"div","props":{"className":"test"},"children":null}"""

        assertEquals(expected, actual, "Unexpected renderer state after update for $type")
    }

    @Test
    fun testUnmount() = withFunctionComponent { type, component ->
        component.unmount()

        assertNull(component.toJSON(), "toJSON() should return null for unmounted components ($type)")
    }

    @Test
    fun testGetInstance() = withFunctionComponent { type, component ->
        assertNull(component.getInstance(), "getInstance() should return null for function components")
    }

    @Test
    fun testCreateNodeMock() {
        @Suppress("LocalVariableName")
        val ComponentWithRef = FC<TestProps> {
            val elem = createRef<Element>()
            val (name, setName) = useState("initial")

            useEffect(name) {
                setName(elem.current?.className ?: "no ref")
            }

            div {
                className = ClassName("test-component")
                ref = elem.unsafeCast<Ref<dom.html.HTMLDivElement>>()

                +name
            }
        }

        fun mock(elem: ReactElement<Props>): Any {
            assertEquals("test-component", elem.props.asDynamic().className)

            return json(Pair("className", "test"))
        }

        lateinit var result: TestRenderer

        act {
            result = render(::mock) {
                ComponentWithRef {}
            }
        }

        val data = result.toJSON().asDynamic()

        assertEquals("test-component", data.props.className)
        assertEquals("test", data.children[0])
    }
}
