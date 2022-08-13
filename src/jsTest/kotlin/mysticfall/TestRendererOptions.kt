package mysticfall

import react.ReactElement

external interface TestRendererOptions {

    var createNodeMock: ((ReactElement) -> Any)?
}
