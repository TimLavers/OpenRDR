package io.rippledown.kb

import io.rippledown.model.KBInfo

sealed class KbResolution {
    data class Exact(val kbInfo: KBInfo) : KbResolution()
    data class Partial(val kbInfo: KBInfo) : KbResolution()
    data class Ambiguous(val name: String, val candidates: List<String>) : KbResolution()
    data class NotFound(val name: String, val available: List<String>) : KbResolution()
}

fun resolveKbName(name: String, kbInfos: Collection<KBInfo>): KbResolution {
    val wanted = name.trim()
    val available = kbInfos.map { it.name }.sorted()
    if (wanted.isEmpty()) return KbResolution.NotFound(wanted, available)

    val exactIgnoringCase = kbInfos.filter { it.name.equals(wanted, ignoreCase = true) }
    when (exactIgnoringCase.size) {
        1 -> return KbResolution.Exact(exactIgnoringCase.single())
        0 -> {}
        else -> {
            val identical = exactIgnoringCase.filter { it.name == wanted }
            return if (identical.size == 1) KbResolution.Exact(identical.single())
            else KbResolution.Ambiguous(wanted, exactIgnoringCase.map { it.name }.sorted())
        }
    }

    val partial = kbInfos.filter { it.name.contains(wanted, ignoreCase = true) }
    return when (partial.size) {
        1 -> KbResolution.Partial(partial.single())
        0 -> KbResolution.NotFound(wanted, available)
        else -> KbResolution.Ambiguous(wanted, partial.map { it.name }.sorted())
    }
}

fun nearDuplicateOf(newName: String, kbInfos: Collection<KBInfo>): KBInfo? {
    val wanted = newName.trim()
    if (wanted.isEmpty()) return null
    return kbInfos
        .filter { !it.name.equals(wanted, ignoreCase = true) }
        .filter { it.name.contains(wanted, ignoreCase = true) || wanted.contains(it.name, ignoreCase = true) }
        .minByOrNull { it.name.length }
}
