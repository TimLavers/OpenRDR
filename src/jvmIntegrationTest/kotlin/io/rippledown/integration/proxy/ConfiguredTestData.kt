package io.rippledown.integration.proxy

import java.io.File

object ConfiguredTestData {
    private val resourcesRoot = "src/jvmIntegrationTest/resources/"

    fun caseFile(caseName: String): File {
        return File("${resourcesRoot}cases/$caseName.json")
    }

    fun kbZipFile(fileName: String): File {
        return File("${resourcesRoot}cases/$fileName.zip")
    }
}