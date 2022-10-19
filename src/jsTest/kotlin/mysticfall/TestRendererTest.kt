package mysticfall

import org.w3c.dom.Element
import react.*
import react.dom.div
import kotlin.js.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestRendererTest : ReactTestSupport {

    fun createWithAct(builder: RBuilder.() -> Unit): TestRenderer {
        lateinit var result: TestRenderer
        act {
            result = render {
                builder()
            }
        }
        return result
    }

    private fun withComponents(block: (ComponentType<TestProps>, TestRenderer) -> Unit) {
        block(TestFuncComponent, createWithAct {
            TestFuncComponent {
                attrs {
                    name = "Function Test"
                }
            }
        })

        block(TestClassComponent::class.react, createWithAct {
            child(TestClassComponent::class) {
                attrs {
                    name = "Class Test"
                }
            }
        })
    }

    private fun withClassComponent(block: (ComponentType<TestProps>, TestRenderer) -> Unit) {
        block(TestClassComponent::class.react, createWithAct {
            child(TestClassComponent::class) {
                attrs {
                    name = "Class Test"
                }
            }
        })
    }

    private fun withFunctionComponent(block: (ComponentType<TestProps>, TestRenderer) -> Unit) {
        println("passed in block $block")

        block(TestFuncComponent, createWithAct {
            TestFuncComponent {
                attrs {
                    name = "Function Test"
                }
            }
        })
    }

    @Test
    fun testRootWithFunctionComponent() {
        val block = { type: ComponentType<TestProps>, component: TestRenderer -> extracted(type, component) }
        withFunctionComponent(block)
    }

    private fun extracted(
        type: ComponentType<TestProps>,
        component: TestRenderer
    ) {
        println("type = ${type}, component = ${component}")
        assertEquals(type, component.root.type, "Unexpected root type for $type")
    }

    @Test
    fun testRoot() = withComponents { type, component ->
        println("type = ${type} component = ${component}")
        assertEquals(type, component.root.type, "Unexpected root type for $type")
    }

    @Test
    fun testFunctionToJSON() = withFunctionComponent { type, component ->
        val expected = """
          |{
          |  "type": "div",
          |  "props": {
          |     "className": "test-component",
          |     "style": {}
          |  },
          |  "children": [
          |     {
          |         "type": "h1",
          |         "props": {
          |             "className": "title",
          |             "style": {}
          |         },
          |         "children": [
          |             "First: Function Test"
          |         ]
          |     }
          |  ]
          |}
        """.trimMargin()

        println("component is fun ${JSON.stringify(component.toJSON())}")

        assertEquals(
            JSON.stringify(JSON.parse(expected)),
            JSON.stringify(component.toJSON()),
            "Unexpected JSON contents for $type"
        )
    }

    @Test
    fun testClassToJSON() = withClassComponent { type, component ->
        val expected = """
          |{
          |  "type": "div",
          |  "props": {
          |     "className": "test-component",
          |     "style": {}
          |  },
          |  "children": [
          |     {
          |         "type": "h1",
          |         "props": {
          |             "className": "title",
          |             "style": {}
          |         },
          |         "children": [
          |             "Updated: Class Test"
          |         ]
          |     }
          |  ]
          |}
        """.trimMargin()

        assertEquals(
            JSON.stringify(JSON.parse(expected)),
            JSON.stringify(component.toJSON()),
            "Unexpected JSON contents for $type"
        )
    }

    @Test
    fun testToJSON() = withComponents { type, component ->

        println("type = ${type} ${component.toJSON()}")
        val expectedFunction = """
          |{
          |  "type": "div",
          |  "props": {
          |     "className": "test-component",
          |     "style": {}
          |  },
          |  "children": [
          |     {
          |         "type": "h1",
          |         "props": {
          |             "className": "title",
          |             "style": {}
          |         },
          |         "children": [
          |             "First: Function Test"
          |         ]
          |     }
          |  ]
          |}
        """.trimMargin()

        val expectedClass = """
          |{
          |  "type": "div",
          |  "props": {
          |     "className": "test-component",
          |     "style": {}
          |  },
          |  "children": [
          |     {
          |         "type": "h1",
          |         "props": {
          |             "className": "title",
          |             "style": {}
          |         },
          |         "children": [
          |             "Updated: Class Test"
          |         ]
          |     }
          |  ]
          |}
        """.trimMargin()
        val isFunction = JSON.stringify(component.toJSON()).contains("Function")
        val expected = if (isFunction) {
            expectedFunction
        } else {
            expectedClass
        }

        assertEquals(
            JSON.stringify(JSON.parse(expected)),
            JSON.stringify(component.toJSON()),
            "Unexpected JSON contents for $type"
        )
    }


    @Test
    fun testToTree() = withComponents { type, component ->
        val actual = component.toTree().asDynamic()

        val message = "Unexpected return value of toTree() for $type"

        assertEquals("component", actual["nodeType"], message)
        assertEquals("host", actual["rendered"]["nodeType"], message)
        assertEquals("div", actual["rendered"]["type"], message)
        assertEquals("title", actual["rendered"]["rendered"][0]["props"]["className"], message)
    }

    @Test
    fun testUpdate() = withComponents { type, component ->
        act {
            update(component) {
                div(classes = "test") {}
            }
        }

        val actual = JSON.stringify(component.toJSON())
        val expected = """{"type":"div","props":{"className":"test","style":{}},"children":null}"""

        assertEquals(expected, actual, "Unexpected component state after update for $type")
    }

    @Test
    fun testUnmount() = withComponents { type, component ->
        component.unmount()

        assertNull(component.toJSON(), "toJSON() should return null for unmounted components ($type)")
    }

    @Test
    fun testGetInstance() = withComponents { type, component ->
        if (type == TestFuncComponent) {
            assertNull(component.getInstance(), "getInstance() should return null for function components")
        } else {
            assertTrue(component.getInstance() is TestClassComponent, "Unexpected type of getInstance()")
        }
    }

    @Test
    fun testCreateNodeMock() {
        @Suppress("LocalVariableName")
        val ComponentWithRef = fc<TestProps> {
            val elem = createRef<Element>()
            val (name, setName) = useState("initial")

            useEffect(name) {
                setName(elem.current?.className ?: "no ref")
            }

            div(classes = "test-component") {
                ref = elem

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
