package npm

@JsModule("debounce")
@JsNonModule
external fun debounce(func: () -> Unit, wait: Int): () -> Unit