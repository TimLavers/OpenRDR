import org.w3c.dom.HTMLFormElement
import react.*
import org.w3c.dom.HTMLInputElement
import react.dom.events.ChangeEventHandler
import react.dom.events.FormEventHandler
import react.dom.html.InputType
import react.dom.html.ReactHTML.form
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.textarea

external interface InputProps : Props {
    var onSubmit: (String) -> Unit
}

val InterpretationView = FC<InputProps> { props ->
    val (text, setText) = useState("")

    val submitHandler: FormEventHandler<HTMLFormElement> = {
        it.preventDefault()
        setText("")
        props.onSubmit(text)
    }

    val changeHandler: ChangeEventHandler<HTMLInputElement> = {
        setText(it.target.value)
    }

    form {
        id = "the_form"
        onSubmit = submitHandler
//        input {
//            type = InputType.text
//            onChange = changeHandler
//            value = text
//        }
        input {
            type = InputType.button
            value = "Submit"
//            onSubmit = submitHandler
        }
    }
    textarea {
        form = "the_form"
    }
}