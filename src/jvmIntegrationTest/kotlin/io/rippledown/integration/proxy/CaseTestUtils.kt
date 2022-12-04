package io.rippledown.integration.proxy

import java.io.File

internal object CaseTestUtils {

    fun caseFile(caseName: String): File {
        return File("src/jvmIntegrationTest/resources/cases/$caseName.json")
    }

    fun writeNewCaseFileToDirectory(caseName: String, directory: File) {
        val contents = caseFile("Case1")
            .readText()
            .replace("Case1", caseName)
        File(directory, "$caseName.json")
            .apply { writeText(contents) }
    }
}