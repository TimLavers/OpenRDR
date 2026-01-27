package io.rippledown.hints

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
    var predicateComponents: List<String> = mutableListOf()
    var signatureComponents: List<String> = mutableListOf()
    while (lineNumber < numberOfLines) {
        val line = nonBlankLines[lineNumber]
        if (line.startsWith(EXPECTED_PREDICATE)) {
            predicateComponents = line
                .replace(EXPECTED_PREDICATE, "")
                .split(",")
                .map { it.trim() }
        } else if (line.startsWith(EXPECTED_SIGNATURE)) {
            signatureComponents = line
                .replace(EXPECTED_SIGNATURE, "")
                    .split(",")
                    .map { it.trim() }
        } else {
            val predicate = FunctionSpecification(predicateComponents[0], predicateComponents.drop(1))
            val signature = if (signatureComponents.isNotEmpty()) {
                FunctionSpecification(signatureComponents[0], signatureComponents.drop(1))
            } else FunctionSpecification()
            val spec = ConditionSpecification(
                userExpression = line,
                attributeName = "x",
                predicate = predicate,
                signature = signature
            )
            result.add(INPUT + line + "\n" + OUTPUT + spec)
        }
        lineNumber++
    }
    return result.joinToString("\n")
}
