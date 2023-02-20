package io.rippledown.integration.proxy

import java.io.File

object ConfiguredTestData {
    private const val resourcesRoot = "src/jvmTest/resources/"

    fun caseFile(caseName: String): File {
        return File("${resourcesRoot}cases/$caseName.json")
    }

    fun kbZipFile(fileName: String): File {
        return File("${resourcesRoot}export/$fileName.zip")
    }
}