package io.rippledown

const val EXPECTED = "Expected: "
const val INPUT = "Input: "
const val OUTPUT = "Output: "

fun trainingSet(fileName: String): String {
    val lines = object {}.javaClass.getResourceAsStream(fileName)?.bufferedReader()!!.readLines()
    var outputLine = ""
    return lines.filter { line -> line.isNotBlank() }
        .mapNotNull { line ->
            val result = if (line.startsWith(EXPECTED, ignoreCase = true)) {
                outputLine = OUTPUT + line.replace(EXPECTED, "", ignoreCase = true)
                null
            } else {
                INPUT + line + "\n" + outputLine
            }
            result?.trim()
        }.joinToString("\n")
}
