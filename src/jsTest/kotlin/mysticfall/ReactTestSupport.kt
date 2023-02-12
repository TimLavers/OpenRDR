@file:Suppress("unused")

package mysticfall

import js.core.jso
import react.*

/**
 * @author Xavier Cho
 * @see <a href="https://github.com/mysticfall/kotlin-react-test">Kotlin API for React Test Renderer</a>
 */
interface ReactTestSupport {
    fun act(block: () -> Unit): Unit = TestRendererGlobal.act(block)

    fun render(block: ChildrenBuilder.() -> Unit) = render(null, block)

    fun render(
        options: TestRendererOptions? = null,
        block: ChildrenBuilder.() -> Unit
    ): TestRenderer {
        val fc = FC<Props> { block() }
        return TestRendererGlobal.create(fc.create(), options).unsafeCast<TestRenderer>()
    }

    fun render(
        mockFactory: (ReactElement<Props>) -> Any,
        block: ChildrenBuilder.() -> Unit
    ): TestRenderer {
        val options = jso<TestRendererOptions> {
            createNodeMock = mockFactory
        }
        return render(options, block)
    }

    fun update(component: TestRenderer, replacement: ChildrenBuilder.() -> Unit) {
        val ui = FC<Props> { replacement() }
        component.update(ui.create())
    }
}
