package io.rippledown.integration.proxy

import java.io.File

internal object CaseTestUtils {

    fun writeNewCaseFileToDirectory(caseName: String, directory: File) {
        val contents = ConfiguredTestData.caseFile("Case1")
            .readText()
            .replace("Case1", caseName)
        File(directory, "$caseName.json")
            .apply { writeText(contents) }
    }
}