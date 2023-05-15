package io.rippledown.textdiff

class TextToAlphabetMapper {

    private val textToAlphaMap = mutableMapOf<String, String>()
    private val startIndex = 'A'.code

    fun toAlpha(text: String): String {
        return textToAlphaMap.getOrPut(text) { (startIndex + textToAlphaMap.size).toChar().toString() }
    }

    fun toText(alpha: String): String {
        return textToAlphaMap.entries.first { it.value == alpha }.key
    }
}