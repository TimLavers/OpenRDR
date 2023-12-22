package io.rippledown.kb.export

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.CaseTestUtils
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class CaseImporterTest: ExporterTestBase() {

    @BeforeEach
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

    @Test
    fun exportImport() {
        val case1 = CaseTestUtils.createCase("Case1")
        val case2 = CaseTestUtils.createCase("Case2")
        val case3 = CaseTestUtils.createCase("Case3")
        CaseExporter(tempDir, listOf(case1, case2, case3)).export()
        val imported = CaseImporter(tempDir).import()
        imported shouldBe setOf(case1, case2, case3)
    }
}