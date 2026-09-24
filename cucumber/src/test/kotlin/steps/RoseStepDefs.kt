package steps

import io.cucumber.java.en.And
import io.rippledown.integration.proxy.ConfiguredTestData.roseCasesFile
import java.io.File
import java.time.Instant
/*
    "gene",
    "driverRole",
    "driverInterpretation",
    "hotspot",
    "biallelic",
//    groupSize,
//    annotations[],
    "effects",
    "codingEffect",
    "hgvsProteinImpact",
    "hgvsCodingImpact",
//    otherImpact,
    "affectedCodon",
    "hrdStatus",
    "isHrdDrivingGene""

findingKey	driverInterpretation
driverLikelihood	driverSource	reportedStatus	gene
driverRole	type	chromosome	position	ref	alt	proteinImpact
codingImpact	codingEffect	effects	codon	exon	transcript	inSpliceRegion	reported
otherProteinImpact	otherCodingImpact	otherEffect	otherEffects	otherCodon	otherExon
otherTranscript	otherInSpliceRegion	otherReported	isCanonical
worstCodingEffect	hotspot
biallelic	biallelicLikelihood	adjustedCopyNumber	adjustedVAF	variantCopyNumber
minorAlleleCopyNumber	alleleReadCount	totalReadCount	genotypeStatus
subclonalLikelihood	somaticLikelihood	clinvarPathogenicity	gnomadFrequency
repeatCount	variantsInGeneSameDriverRole	biallelicStatus	isHrdDrivingGene

 */
val IMPORTANT_COLUMNS = listOf(
    "gene",
    "driverRole",
    "driverInterp",
    "hotspot",
    "biallelic",
    "effects",
    "codingEffect",
    "proteinImpact",
    "codingImpact",
    "codon",
    "hrdStatus",
    "isHrdDrivingGene"
)

data class RoseCaseRow(val attributeValues: Map<String, String>) {
    fun importantData(): Map<String, String> {
        return attributeValues.filterKeys { key -> key in IMPORTANT_COLUMNS }
    }
}

fun main(args: Array<String>) {
    parseRoseCaseRows(File("/Users/timlavers/tgl/code/OpenRDR/cucumber/src/test/resources/rose/cases2.tsv")).forEachIndexed { index, row ->
        val caseNameBaseToCount = mutableMapOf<String, Int>()

        val gene = row.attributeValues["gene"]!!
        val impact = row.attributeValues["proteinImpact"]!!
        val caseNameBase = "$gene $impact"
        val caseCountForGene = caseNameBaseToCount.getOrDefault(caseNameBase, 0) + 1
        caseNameBaseToCount[caseNameBase] = caseCountForGene
        val caseName = "$caseNameBase $caseCountForGene"
        val dataLine = IMPORTANT_COLUMNS.map{row.attributeValues[it] }.joinToString(separator = ",")
        println("$caseName,$dataLine")
    }

}

// All rows are given the same arbitrary episode date/time: the actual value
// is not significant, only that every attribute of every case shares it.
private val ARBITRARY_EPISODE_TIME = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli()

class RoseStepDefs {

    @And("I send a case to {string} for each row in the rose cases file")
    fun sendACaseForEachRowInTheRoseCasesFile(kbName: String) {
        val caseNameBaseToCount = mutableMapOf<String, Int>()
        parseRoseCaseRows(roseCasesFile()).forEachIndexed { index, row ->
            val gene = row.attributeValues["gene"]!!
            val impact = row.attributeValues["proteinImpact"]!!
            val caseNameBase = "$gene $impact"
            val caseCountForGene = caseNameBaseToCount.getOrDefault(caseNameBase, 0) + 1
            caseNameBaseToCount[caseNameBase] = caseCountForGene
            val caseName = "$caseNameBase $caseCountForGene"
            labProxy().provideCaseForKb(kbName, caseName, row.importantData(), ARBITRARY_EPISODE_TIME)
        }
    }
}

private fun parseRoseCaseRows(file: File): List<RoseCaseRow> {
    val lines = file.readLines().filter { it.isNotBlank() }
    val headers = lines.first().split("\t")
    return lines.drop(1).map { line ->
        val values = line.split("\t")
        require(values.size == headers.size) {
            "Expected ${headers.size} fields but found ${values.size} in line: $line"
        }
        RoseCaseRow(headers.zip(values).toMap())
    }
}
