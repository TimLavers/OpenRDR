package io.rippledown.appbar

import java.io.File

/** File operations on knowledge bases; other management is handled through chat. */
interface KBControlHandler {
    var importKB: (data: File) -> Unit
    var exportKB: (data: File) -> Unit
}
