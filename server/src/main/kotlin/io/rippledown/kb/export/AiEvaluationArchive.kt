package io.rippledown.kb.export

import io.rippledown.kb.ai.validateAiBoolean
import io.rippledown.model.Attribute
import io.rippledown.model.rule.evaluation.AI_FINGERPRINT_VERSION
import io.rippledown.model.rule.evaluation.AiEvaluationRecord
import io.rippledown.model.rule.evaluation.canonicalFingerprint
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class ExportedAiEvaluation(
    val formatVersion: Int,
    val fingerprintVersion: Int,
    val record: AiEvaluationRecord
)

/** Validated independently of persistence so a bad archive cannot create a partial KB. */
internal object AiEvaluationArchive {
    fun export(directory: File, records: Collection<AiEvaluationRecord>) {
        checkDirectoryIsSuitableForExport(directory, "AI evaluations")
        directory.mkdirs()
        records.sortedBy { it.key.asString() }.forEachIndexed { index, record ->
            File(directory, "$index.json").writeText(Json.encodeToString(
                ExportedAiEvaluation(1, AI_FINGERPRINT_VERSION, record)))
        }
    }

    fun read(directory: File, attributes: Set<Attribute>): List<AiEvaluationRecord> {
        if (!directory.exists()) return emptyList()
        require(directory.isDirectory) { "AI evaluations must be a directory." }
        val records = directory.listFiles().orEmpty().map { file ->
            require(file.isFile) { "An AI evaluation entry must be a file." }
            val exported = Json.decodeFromString<ExportedAiEvaluation>(file.readText())
            require(exported.formatVersion == 1 && exported.fingerprintVersion == AI_FINGERPRINT_VERSION) {
                "Unsupported AI evaluation archive or fingerprint version."
            }
            exported.record.also { record ->
                validateAiBoolean(record.definition, attributes)
                require(record.key.revision == record.definition.revision) { "AI evaluation revision does not match its definition." }
                require(record.key.definitionFingerprint == record.definition.canonicalFingerprint()) {
                    "AI evaluation definition fingerprint does not match its definition."
                }
                // Inputs are intentionally not stored. Validate the hash representation; historical
                // and preview records need not correspond to any currently retained case.
                require(record.key.inputHash.matches(Regex("[0-9a-f]{64}"))) { "Invalid AI evaluation input hash." }
                require(record.requestedModel == record.definition.evaluationProfile.model) {
                    "AI evaluation model does not match its saved profile."
                }
                require(record.evaluatedAt >= 0) { "Invalid AI evaluation timestamp." }
            }
        }
        records.groupBy { it.key }.forEach { (key, duplicates) ->
            require(duplicates.distinct().size == 1) { "Conflicting AI evaluation records for ${key.asString()}." }
        }
        return records.distinctBy { it.key }
    }
}
