package npm

@JsModule("debounce")
@JsNonModule
external fun <T> debounce(func: T, wait: Int): T

fun <T> debounce(func: T, wait: Long): T = debounce(func, wait.toInt())
