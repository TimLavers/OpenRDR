package io.rippledown.llm

import io.rippledown.conditiongenerator.spec

const val EXPECTED_PREDICATE = "Expected predicate: "
const val INPUT = "Input: "
const val OUTPUT = "Output: "

/**
 * Generate a list of input/output examples for the LLM from a training set file where there
 * is one expected output corresponding to several inputs. For example:
 *
 * EXPECTED PREDICATE: High
 * elevated x
 * excessive x
 * x is high
 */
fun trainingSet(fileName: String): String {
    val lines = object {}.javaClass.getResourceAsStream(fileName)?.bufferedReader()!!.readLines()
    var outputLine = ""
    return lines.filter { line -> line.isNotBlank() }
        .mapNotNull { line ->
            val result = if (line.startsWith(EXPECTED_PREDICATE, ignoreCase = true)) {
                val components = line
                    .replace(EXPECTED_PREDICATE, "", ignoreCase = true)
                    .split(",")
                    .map { it.trim() }
                val spec = spec(
                    predicateName = components[0],
                    predicateParameters = components.drop(1)
                )
                outputLine = OUTPUT + spec
                null
            } else {
                INPUT + line + "\n" + outputLine
            }
            result?.trim()
        }.joinToString("\n")
}
