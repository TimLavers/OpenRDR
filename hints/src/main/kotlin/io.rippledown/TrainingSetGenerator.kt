package io.rippledown

import java.io.File

const val EXPECTED = "Expected: "
const val INPUT = "Input: "
const val OUTPUT = "Output: "

fun trainingSet(file: File) = with(file) {
    var outputLine = ""
    readLines()
        .filter { line -> line.isNotBlank() }
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
