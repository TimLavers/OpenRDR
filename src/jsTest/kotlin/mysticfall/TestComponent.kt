package mysticfall

import react.*
import react.dom.div
import react.dom.h1

external interface TestProps : Props {
    var name: String
}

external interface TestState : State {
    var name: String
}

val TestFuncComponent = fc<TestProps> { props ->
    val (name, setName) = useState(props.name)

//    useEffect(name) {
//        setName("Updated: ${props.name}")
//    }
    useEffectOnce { setName("First: ${props.name}") }

    div(classes = "test-component") {
        h1(classes = "title") {
            +name
        }
    }
}
val TestFuncComponentA = fc<TestProps> { props ->
    val (name, setName) = useState(props.name)

    useEffect(name) {
        println(
            "TestFuncComponentA: useEffect: name = $name"
        )
        setName("Updated: ${props.name}")
    }

    div {
        +name
    }
}

class TestClassComponent(props: TestProps) : RComponent<TestProps, TestState>(props) {

    override fun TestState.init(props: TestProps) {
        name = props.name
    }

    override fun componentDidMount() {
        setState {
            name = "Updated: ${props.name}"
        }
    }

    override fun RBuilder.render() {
        div(classes = "test-component") {
            h1(classes = "title") {
                +state.name
            }
        }
    }
}
