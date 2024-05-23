package io.rippledown.integration.proxy

import java.io.File

object ConfiguredTestData {
    private const val resourcesRoot = "src/test/resources/"

    fun caseFile(caseName: String): File {
        return File("${resourcesRoot}cases/$caseName.json")
    }

    fun kbZipFile(fileName: String): File {
        return File("${resourcesRoot}export/$fileName.zip")
    }

    fun testDataFile(relativePath: String) = File("$resourcesRoot/$relativePath")
}