package io.rippledown.llm

import io.rippledown.conditiongenerator.ConditionSpecification
import io.rippledown.conditiongenerator.FunctionSpecification
import io.rippledown.conditiongenerator.spec

const val EXPECTED_PREDICATE = "EXPECTED PREDICATE:"
const val EXPECTED_SIGNATURE = "EXPECTED SIGNATURE:"
const val INPUT = "Input: "
const val OUTPUT = "Output: "

/**
 * Generate a list of input/output examples for the LLM from a list of lines where there
 * is one expected output corresponding to several inputs. For example:
 *
 * EXPECTED PREDICATE: High
 * elevated x
 * excessive x
 * x is high
 */
fun examplesFrom(lines: List<String>): String {
    val nonBlankLines = lines.filter { line -> line.isNotBlank() }
    val numberOfLines = nonBlankLines.size
    var lineNumber = 0
    val result = mutableListOf<String>()
    var spec = ConditionSpecification()
    while (lineNumber < numberOfLines) {
        val line = nonBlankLines[lineNumber]
        if (line.startsWith(EXPECTED_PREDICATE)) {
            val predicateComponents = line
                .replace(EXPECTED_PREDICATE, "")
                .split(",")
                .map { it.trim() }
            spec = spec(
                predicateName = predicateComponents[0],
                predicateParameters = predicateComponents.drop(1),
                signatureName = "",
            )
        } else if (line.startsWith(EXPECTED_SIGNATURE)) {
            val signatureComponents = line
                .replace(EXPECTED_SIGNATURE, "")
                    .split(",")
                    .map { it.trim() }
            spec.signature = FunctionSpecification(signatureComponents[0], signatureComponents.drop(1))
        } else {
            result.add(INPUT + line + "\n" + OUTPUT + spec)
        }
        lineNumber++
    }
    return result.joinToString("\n")
}
