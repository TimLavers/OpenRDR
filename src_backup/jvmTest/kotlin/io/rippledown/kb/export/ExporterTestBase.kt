package io.rippledown.kb.export

import io.rippledown.model.rule.RuleTestBase
import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.charset.Charset
import kotlin.io.path.createTempDirectory

open class ExporterTestBase: RuleTestBase() {
    var tempDir: File = createTempDirectory().toFile()

    @Before
    open fun init() {
        tempDir.mkdirs()
    }

    fun writeFileInDirectory(file: File): File {
        val textFile = File(file, "blah.txt")
        FileUtils.writeStringToFile(textFile, "Whatever", Charset.defaultCharset())
        return textFile
    }
}