import kotlin.js.Date

fun debug(msg: String) {
    println("\n\n${Date().toISOString()} $msg")
}
