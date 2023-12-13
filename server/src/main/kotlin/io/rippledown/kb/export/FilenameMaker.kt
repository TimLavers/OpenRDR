package io.rippledown.kb.export

import kotlin.random.Random


/**
 * Creates legal filenames from, the given strings, avoiding
 * duplicate names. The filenames should resemble the original
 * names.
 */
class FilenameMaker(private val originalNames: Set<String>) {
    private val badChars = """[<>/:\\"|?*.]""".toRegex()
    fun makeUniqueNames(suffix: String = ".json"): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val namesMadeSoFar = mutableListOf<String>()
        originalNames.forEach{
            val candidate = it.replace(badChars, "_")
            val candidateLC = candidate.lowercase()
            val nameRoot = if (namesMadeSoFar.contains(candidateLC)) candidate + Random.nextInt() else candidate
            namesMadeSoFar.add(nameRoot.lowercase())
            result[it] = nameRoot + suffix
        }
        return result
    }
}