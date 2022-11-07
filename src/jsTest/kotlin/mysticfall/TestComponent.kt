package mysticfall

import csstype.ClassName
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.h1
import react.useEffectOnce
import react.useState

external interface TestProps : Props {
    var name: String
}

val TestFuncComponent = FC<TestProps> { props ->
    val (name, setName) = useState(props.name)

    useEffectOnce { setName("First: ${props.name}") }

    div {
        className = ClassName("test-component")
        h1 {
            className = ClassName("title")
            +name
        }
    }
}
