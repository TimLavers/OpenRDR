package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.model.KBInfo
import io.rippledown.persistence.InMemoryKB
import org.junit.Before
import java.io.File
import kotlin.test.Test

class KBExporterTest: ExporterTestBase() {
    private lateinit var kb: KB

    @Before
    override fun init() {
        super.init()
        tempDir.mkdirs()
        kb = KB(InMemoryKB(KBInfo("Thyroids")))
    }

    @Test
    fun `destination should be a directory`() {
        val textFile = writeFileInDirectory(tempDir)
        shouldThrow<IllegalArgumentException>{
            KBExporter(textFile, kb)
        }.message shouldBe "KB export destination is not a directory."
    }

    @Test
    fun `destination should be empty`() {
        val directory = File(tempDir, "exportDir")
        directory.mkdirs()
        writeFileInDirectory(directory)
        shouldThrow<IllegalArgumentException>{
            KBExporter(directory, kb)
        }.message shouldBe "KB export directory is not empty."
    }

    @Test
    fun `destination should be exist`() {
        val directory = File(tempDir, "exportDir")
        shouldThrow<IllegalArgumentException>{
            KBExporter(directory, kb)
        }.message shouldBe "KB export destination is not an existing directory."
    }

    @Test
    fun `export empty kb`() {
        KBExporter(tempDir, kb).export()
    }
}