package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Before
import kotlin.test.Test

class CaseImporterTest: ExporterTestBase() {

    @Before
    override fun init() {
        super.init()
        tempDir.mkdirs()
    }

    @Test
    fun `source should be an existing directory`() {
        val textFile = writeFileInDirectory(tempDir)
        shouldThrow<IllegalArgumentException>{
            CaseImporter(textFile)
        }.message shouldBe "$textFile is not an existing directory."
    }

    @Test
    fun `empty directory`() {
        val imported = CaseImporter(tempDir).import()
        imported.size shouldBe 0
    }
}