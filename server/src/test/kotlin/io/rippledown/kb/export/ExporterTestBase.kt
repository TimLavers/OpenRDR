package io.rippledown.kb.export

import io.rippledown.model.rule.RuleTestBase
import org.junit.jupiter.api.BeforeEach
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory

open class ExporterTestBase: RuleTestBase() {
    var tempDir: Path = createTempDirectory()

    @BeforeEach
    open fun init() {
        tempDir.createDirectories()
    }

    fun writeFileInDirectory(directory: Path): Path {
        val textFile = directory.resolve("blah.txt")
        Files.writeString(textFile, "Whatever")
        return textFile
    }
}