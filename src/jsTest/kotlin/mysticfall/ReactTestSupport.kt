@file:Suppress("unused")

package mysticfall

import kotlinx.js.jso
import react.Props
import react.RBuilder
import react.RBuilderSingle
import react.ReactElement

/**
 * @author Xavier Cho
 * @see <a href="https://github.com/mysticfall/kotlin-react-test">Kotlin API for React Test Renderer</a>
 */
interface ReactTestSupport {
    fun ReactTestSupport.act(block: () -> Unit): Unit = TestRendererGlobal.act(block)

    fun ReactTestSupport.trender(block: RBuilder.() -> Unit): TestRenderer = render(null, block)

    fun ReactTestSupport.render(
        options: TestRendererOptions? = null,
        block: RBuilder.() -> Unit
    ): TestRenderer {
        val builder = RBuilderSingle()

        block(builder)

        return TestRendererGlobal.create(builder.childList.first(), options).unsafeCast<TestRenderer>()
    }

    fun ReactTestSupport.render(
        mockFactory: (ReactElement<Props>) -> Any,
        block: RBuilder.() -> Unit
    ): TestRenderer {
        val options = jso<TestRendererOptions> {
            createNodeMock = mockFactory
        }

        return render(options, block)
    }


    fun ReactTestSupport.update(component: TestRenderer, replacement: RBuilder.() -> Unit) {
        val builder = RBuilderSingle()

        replacement(builder)

        component.update(builder.childList.first())
    }
}
