@file:JsModule("react-dom/test-utils")
@file:JsNonModule

package react.dom

import react.ComponentType


external fun isCompositeComponentWithType(
    element: Any?,
    type: ComponentType<*>
): Boolean
