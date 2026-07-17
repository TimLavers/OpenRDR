package io.rippledown.kb.export

import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import java.io.File
import kotlin.test.Test

class AttributesImporterTest : ExporterTestBase() {

    @Test
    fun exportEmpty() {
        val textFile = File(tempDir,"Attributes.txt")
        AttributesExporter(textFile, emptySet()).export()
        val recovered = AttributesImporter(textFile).import()
        recovered shouldBe emptyMap()
    }

    @Test
    fun exportImport() {
        val tsh = Attribute(100, "TSH")
        val ft3 = Attribute(200, "FT3")
        val ft4 = Attribute(300, "FT4")
        val attributeSet = setOf(tsh, ft4, ft3)
        val textFile = File(tempDir,"Attributes.txt")
        AttributesExporter(textFile, attributeSet).export()
        val recovered = AttributesImporter(textFile).import()
        recovered.size shouldBe 3
        recovered[tsh.id] shouldBe tsh
        recovered[ft3.id] shouldBe ft3
        recovered[ft4.id] shouldBe ft4
    }

    @Test
    fun `attribute kinds survive export and import`() {
        // Given attributes of each kind
        val glucose = Attribute(100, "Glucose")
        val bmi = Attribute(200, "BMI", AttributeKind.DERIVED)
        val comment = Attribute(300, "DiabetesStatus", AttributeKind.COMMENT)
        val textFile = File(tempDir, "Attributes.txt")

        // When they are exported and imported
        AttributesExporter(textFile, setOf(glucose, bmi, comment)).export()
        val recovered = AttributesImporter(textFile).import()

        // Then the kinds are unchanged
        recovered[glucose.id]!!.kind shouldBe AttributeKind.EXTERNAL
        recovered[bmi.id]!!.kind shouldBe AttributeKind.DERIVED
        recovered[comment.id]!!.kind shouldBe AttributeKind.COMMENT
    }

    @Test
    fun `attribute names containing spaces survive export and import`() {
        // Given an attribute whose name contains spaces
        val clinicalNotes = Attribute(100, "Clinical Notes")
        val textFile = File(tempDir, "Attributes.txt")

        // When it is exported and imported
        AttributesExporter(textFile, setOf(clinicalNotes)).export()
        val recovered = AttributesImporter(textFile).import()

        // Then the name is unchanged
        recovered[clinicalNotes.id]!!.name shouldBe "Clinical Notes"
    }

    @Test
    fun `a legacy format export can still be imported`() {
        // Given an attributes file in the legacy "id name" format
        val textFile = File(tempDir, "Attributes.txt")
        textFile.writeText("100 TSH\n200 Clinical Notes\n")

        // When it is imported
        val recovered = AttributesImporter(textFile).import()

        // Then the attributes are recovered as external attributes
        recovered.size shouldBe 2
        recovered[100]!!.name shouldBe "TSH"
        recovered[100]!!.kind shouldBe AttributeKind.EXTERNAL
        recovered[200]!!.name shouldBe "Clinical Notes"
        recovered[200]!!.kind shouldBe AttributeKind.EXTERNAL
    }
}