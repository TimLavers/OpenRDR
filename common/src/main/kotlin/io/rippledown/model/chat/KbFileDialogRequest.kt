package io.rippledown.model.chat

import io.rippledown.model.KBInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface KbFileDialogRequest {
    val requestId: String

    @Serializable
    @SerialName("import")
    data class Import(override val requestId: String) : KbFileDialogRequest

    @Serializable
    @SerialName("export")
    data class Export(override val requestId: String, val kbInfo: KBInfo) : KbFileDialogRequest
}
