package io.rippledown.kb

import io.rippledown.model.KBInfo
import io.rippledown.sample.SampleKB

sealed class KbResolution {
    data class Exact(val kbInfo: KBInfo) : KbResolution()
    data class Partial(val kbInfo: KBInfo) : KbResolution()
    data class Demonstration(val sample: SampleKB) : KbResolution()
    data class Ambiguous(val name: String, val candidates: List<String>) : KbResolution()
    data class NotFound(
        val name: String,
        val available: List<String>,
        val demonstrations: List<String> = emptyList()
    ) : KbResolution()
}

fun resolveKbName(
    name: String,
    kbInfos: Collection<KBInfo>,
    demonstrations: Collection<SampleKB> = emptyList()
): KbResolution {
    val wanted = name.trim()
    val available = kbInfos.map { it.name }.sorted()
    val demoTitles = demonstrations.map { it.title() }.sorted()
    if (wanted.isEmpty()) return KbResolution.NotFound(wanted, available, demoTitles)

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

    demonstrations.firstOrNull { it.title().equals(wanted, ignoreCase = true) }?.let {
        return KbResolution.Demonstration(it)
    }

    val partial = kbInfos.filter { it.name.contains(wanted, ignoreCase = true) }
    val demoMatches = demonstrations.filter { it.title().contains(wanted, ignoreCase = true) }
    when (partial.size) {
        1 -> return KbResolution.Partial(partial.single())
        0 -> {}
        else -> return KbResolution.Ambiguous(
            wanted,
            (partial.map { it.name } + demoMatches.map { it.title() }).sorted()
        )
    }
    return when (demoMatches.size) {
        1 -> KbResolution.Demonstration(demoMatches.single())
        0 -> KbResolution.NotFound(wanted, available, demoTitles)
        else -> KbResolution.Ambiguous(wanted, demoMatches.map { it.title() }.sorted())
    }
}

fun isDemonstrationTitle(name: String, demonstrations: Collection<SampleKB>): Boolean =
    demonstrations.any { it.title().equals(name.trim(), ignoreCase = true) }

fun nearDuplicateOf(newName: String, kbInfos: Collection<KBInfo>): KBInfo? {
    val wanted = newName.trim()
    if (wanted.isEmpty()) return null
    return kbInfos
        .filter { !it.name.equals(wanted, ignoreCase = true) }
        .filter { it.name.contains(wanted, ignoreCase = true) || wanted.contains(it.name, ignoreCase = true) }
        .minByOrNull { it.name.length }
}
