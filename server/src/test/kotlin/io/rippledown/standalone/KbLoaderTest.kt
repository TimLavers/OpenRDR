package io.rippledown.standalone

import io.kotest.matchers.shouldBe
import io.rippledown.kb.export.KBExporter
import io.rippledown.kb.export.util.Zipper
import java.io.File
import kotlin.io.path.createDirectory
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.writeBytes
import kotlin.test.BeforeTest
import kotlin.test.Test

class KbLoaderTest: StandAloneInterpreterTestBase() {
    private lateinit var kbLoader: KbLoader
    private lateinit var tempDir: File

    @BeforeTest
    override fun setup() {
        super.setup()
        tempDir = createTempDirectory().toFile()
    }

    @Test
    fun `should load a kb`() {
        val exportDir = tempDir.resolve("export").toPath()
        exportDir.createDirectory()
        KBExporter(exportDir, kb).export()
        val zipBytes = Zipper(exportDir.toFile()).zip()
        val zipFile = createTempFile("import", ".zip")
        zipFile.writeBytes(zipBytes)

        kbLoader = KbLoader(zipFile)
        with(kbLoader.getInterpreter()) {
            val inputs = mapOf(a.name to valueBlah, b.name to "ignored", c.name to valueSuch)
            val interpretation = interpretStringMap(inputs)
            interpretation shouldBe COMMENT_3 + "\n" + COMMENT_1
        }
    }
}