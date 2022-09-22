import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mysticfall.TestInstance
import mysticfall.TestRenderer


fun TestRenderer.findById(id: String): TestInstance<*> {
    val testInstance = root.findAll {
        it.props.asDynamic()["id"] == id
    }[0]

    return if (testInstance != undefined) {
        testInstance
    } else {
        throw Error("Instance with id \"$id\" not found")
    }
}

fun TestInstance<*>.text() = props.asDynamic()["children"][0].unsafeCast<String>()

suspend fun click(testInstance: TestInstance<*>) =
    coroutineScope {
        launch {
            withContext(Default) {
                testInstance.props.asDynamic().onClick() as Unit
            }
        }.join()
    }

