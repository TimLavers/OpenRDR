package mysticfall

import csstype.ClassName
import kotlinx.js.jso
import proxy.printJSON
import react.dom.html.ReactHTML.a
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.dom.html.ReactHTML.h5
import react.dom.html.ReactHTML.p
import kotlin.js.json
import kotlin.test.*

class TestInstanceTest : ReactTestSupport {

    @Test
    fun testFind() {
        val renderer = render {
            div {
                div {
                    +"Div 1"
                }
                div {
                    +"Div 3"

                    div {
                        className = ClassName("selected")
                        +"Div 4"
                    }
                }
            }
        }
        val selected = renderer.root.find { c -> c.props.asDynamic()["className"] == "selected" }
        assertEquals("Div 4", selected.props.asDynamic()["children"])
    }

    @Test
    fun testFindNoMatch() {
        val renderer = render {
            div {}
        }

        assertFailsWith<Throwable> {
            renderer.root.find { false }
        }
    }

    @Test
    fun testFindAll() {
        val renderer = render {
            div {
                div {
                    className = ClassName("selected")
                    +"Div 1"
                }
                div {
                    +"Div 2"
                }
                div {
                    +"Div 3"

                    div {
                        className = ClassName("selected")
                        +"Div 4"
                    }
                }
            }
        }

        val selected = renderer.root
            .findAll { c -> c.props.asDynamic()["className"] == "selected" }
            .map { c -> c.props.asDynamic()["children"] }

        assertContentEquals(listOf("Div 1", "Div 4"), selected)
    }

    @Test
    fun testFindAllNoMatch() {
        val renderer = render {
            div { }
        }

        assertTrue(renderer.root.findAll { false }.isEmpty())
    }


    @Test
    fun testFindByTypeFunction() {
        val renderer = render {
            div {
                +"Div 2"

                TestFuncComponent {
                    name = "Function Component"
                }
            }
        }

        assertEquals(
            "Function Component",
            renderer.root.findByType(TestFuncComponent).props.name
        )
    }

    @Test
    fun testFindByTypeSimpleFunction() {
        lateinit var renderer: TestRenderer
        act {
            renderer = render {
                div {
                    TestFuncComponent {
                        name = "Function Component"
                    }
                }
            }
        }
        val component = renderer.root.findByType("div")!!
        assertEquals(1, component.children.size)

        val name = component.children[0].props.asDynamic()["name"]
        assertEquals("Function Component", name)
    }

    @Test
    fun testFindByTypeNoMatch() {
        val renderer = render {
            div {}
        }

        assertFailsWith<Throwable> {
            renderer.root.findByType(TestFuncComponent)
        }
    }

    @Test
    fun testFindByTypeMultipleMatches() {
        val renderer = render {
            div {
                TestFuncComponent {}
                TestFuncComponent {}
            }
        }

        assertFailsWith<Throwable> {
            renderer.root.findByType(TestFuncComponent)
        }
    }

    @Test
    fun testFindByTypeLiteral() {
        val renderer = render {
            div {
                p {
                    +"Paragraph"
                }
                div {
                    +"Div 2"

                    h5 {
                        +"Heading"
                    }
                }
            }
        }

        assertEquals(
            "Paragraph",
            renderer.root.findByType("p")?.props.asDynamic()["children"]
        )

        assertEquals(
            "Heading",
            renderer.root.findByType("h5")?.props.asDynamic()["children"]
        )
    }

    @Test
    fun testFindByTypeLiteralNoMatch() {
        val renderer = render {
            div {}
        }

        assertFailsWith<Throwable> {
            renderer.root.findByType("h1")
        }
    }

    @Test
    fun testFindByTypeLiteralMultipleMatches() {
        val renderer = render {
            div {
                h1 {}
                h1 {}
            }
        }

        assertFailsWith<Throwable> {
            renderer.root.findByType("h1")
        }
    }

    @Test
    fun testFindAllByType() {
        val renderer = render {
            div {
                div {
                    +"Div 1"
                }
                TestFuncComponent {
                    name = "Component 1"
                }
                div {
                    +"Div 2"

                    TestFuncComponent {
                        name = "Component 2"
                    }
                }
            }
        }

        val names = renderer.root.findAllByType(TestFuncComponent).map { c -> c.props.name }

        assertContentEquals(listOf("Component 1", "Component 2"), names)
    }

    @Test
    fun testFindAllByTypeNoMatch() {
        val renderer = render {
            div {}
        }

        assertTrue(renderer.root.findAllByType(TestFuncComponent).isEmpty())
    }

    @Test
    fun testFindAllByTypeLiteral() {
        val renderer = render {
            div {
                div {
                    h1 {
                        +"Heading 1"
                    }
                }
                h1 {
                    +"Heading 2"
                }
            }
        }

        val names = renderer.root.findAllByType("h1").map { c -> c.props.asDynamic()["children"] }

        assertContentEquals(listOf("Heading 1", "Heading 2"), names)
    }

    @Test
    fun testFindAllByTypeLiteralNoMatch() {
        val renderer = render {
            div {}
        }

        assertTrue(renderer.root.findAllByType("p").isEmpty())
    }

    @Test
    fun testFindByProps() {
        val renderer = render {
            div {
                div {
                    +"Div 1"
                }
                div {
                    TestFuncComponent {
                        name = "Component 1"
                    }
                }
            }
        }

        val props = jso<TestProps> {
            name = "Component 1"
        }

        assertEquals("Component 1", renderer.root.findByProps(props).props.name)
    }

    @Test
    fun testFindByPropsMultipleMatches() {
        val renderer = render {
            TestFuncComponent {
                name = "Component 1"
            }
            TestFuncComponent {
                name = "Component 1"
            }
        }

        val props = jso<TestProps> {
            name = "Component 1"
        }

        assertFailsWith<Throwable> {
            renderer.root.findByProps(props)
        }
    }

    @Test
    fun testFindByPropsNoMatch() {
        val renderer = render {
            div {
                div {
                    TestFuncComponent {
                        name = "Component 1"
                    }
                }
            }
        }

        val props = jso<TestProps> {
            name = "Component 2"
        }

        assertFailsWith<Throwable> {
            renderer.root.findByProps(props)
        }
    }

    @Test
    fun testFindByPropsJson() {
        val renderer = render {
            div {
                a {
                    className = ClassName("selected")
                    href = "#anchor1"
                    +"Link 1"
                }
                a {
                    href = "#anchor1"
                    +"Link 2"
                }
                a {
                    className = ClassName("selected")
                    +"Link 3"
                }
            }
        }

        val props = json(Pair("className", "selected"), Pair("href", "#anchor1"))

        val component = renderer.root.findByProps(props)

        assertEquals("a", component.type.toString())
        assertEquals("Link 1", component.props.asDynamic()["children"].toString())
    }

    @Test
    fun testFindByPropsJsonNoMatch() {
        val renderer = render {
            div {
                a {
                    className = ClassName("selected")
                    +"Link 1"
                }
            }
        }

        val props = json(Pair("href", "#anchor1"))

        assertFailsWith<Throwable> {
            renderer.root.findByProps(props)
        }
    }

    @Test
    fun testFindByPropsJsonMultipleMatches() {
        val renderer = render {
            div {
                a {
                    className = ClassName("selected")
                    +"Link 1"
                }
                a {
                    className = ClassName("selected")
                    +"Link 2"
                }
            }
        }

        val props = json(Pair("className", "selected"))

        assertFailsWith<Throwable> {
            renderer.root.findByProps(props)
        }
    }

    @Test
    fun testFindAllByProps() {
        val renderer = render {
            div {
                TestFuncComponent {
                    name = "Component 1"
                }
                div {
                    TestFuncComponent {
                        name = "Component 1"
                    }
                }
                TestFuncComponent {
                    name = "Component 2"
                }
            }
        }

        val props = jso<TestProps> {
            name = "Component 1"
        }

        assertEquals(2, renderer.root.findAllByProps(props).size)
    }

    @Test
    fun testFindAllByPropsNoMatch() {
        val renderer = render {
            div {}
        }

        val props = jso<TestProps> {
            name = "Component 1"
        }

        assertTrue(renderer.root.findAllByProps(props).isEmpty())
    }

    @Test
    fun testFindAllByPropsJsonNoMatch() {
        val renderer = render {
            div {
                a {
                    className = ClassName("selected")
                    href = "#anchor1"
                }
            }
        }

        val props = json(Pair("className", "selected"), Pair("href", "#anchor2"))

        assertTrue(renderer.root.findAllByProps(props).isEmpty())
    }

    @Test
    fun testInstance() {
        val renderer = render {
            div {
                TestFuncComponent {
                    name = "Function Component"
                }
            }
        }

        val funcComp = renderer.root.findByProps(json(Pair("name", "Function Component")))
        assertNull(funcComp.instance)
    }

    @Test
    fun testType() {
        val renderer = render {
            div {
                TestFuncComponent {
                    name = "Function Component"
                }
            }
        }
        val funcComp = renderer.root.findByProps(json(Pair("name", "Function Component")))
        assertEquals(funcComp.type, TestFuncComponent)
    }

    @Test
    fun testProps() {
        val renderer = render {
            div {
                h5 {
                    className = ClassName("title")
                    +"Heading"
                }
                TestFuncComponent {
                    name = "Function Component"
                }
            }
        }

        val h5 = renderer.root.findByType("h5")?.props.asDynamic()

        assertEquals("title", h5["className"])
        assertEquals("Heading", h5["children"])

        val name = renderer.root.findByType(TestFuncComponent).props.name

        assertEquals("Function Component", name)
    }

    @Test
    fun testGrandParent() {
        val renderer = render {
            div {
                div {
                    className = ClassName("header")
                    p {
                        +"Body"
                    }
                }
            }
        }
        val p = renderer.root.findByType("p")!!
        assertEquals("header", p.parent!!.props.asDynamic()["className"])
    }

    @Test
    fun testParent() {
        val renderer = render {
            div {
                className = ClassName("header")
                p {
                    +"Body"
                }
            }
        }
        renderer.printJSON()

        val p = renderer.root.findByType("p")!!
        val parent = p.parent!!
        assertEquals("header", parent.props.asDynamic()["className"])
    }

    @Test
    fun testChildren() {
        val renderer = render {
            div {
                div {
                    h1 {
                        +"Heading 1"
                    }
                    h1 {
                        +"Heading 2"
                    }
                }
                div {}
                div {}
            }
        }
        val divParent = renderer.root.children[0]
        assertEquals(3, divParent.children.size)

        val divFirstChild = divParent.children[0]
        val headings = divFirstChild.children.map { c -> c.props.asDynamic()["children"] }
        assertContentEquals(listOf("Heading 1", "Heading 2"), headings)
    }
}
