@file:JsModule("react-dom/test-utils")
@file:JsNonModule

package react.dom


external fun findAllInRenderedTree(
    element: Any?,
    test: (element: Any) -> Boolean
): Array<Any>
