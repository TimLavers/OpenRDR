package io.rippledown.persistence.postgres

import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.evaluation.AiEvaluationKey
import io.rippledown.model.rule.evaluation.AiEvaluationRecord
import io.rippledown.persistence.AiEvaluationStore
import io.rippledown.persistence.expressionFromString
import io.rippledown.persistence.expressionToString
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

const val AI_EVALUATIONS_TABLE = "ai_evaluations"

/**
 * The unique index on the key columns makes [insertIfAbsent] atomic: a
 * racing duplicate is ignored by the database, and the record then read
 * back is the one every caller sees.
 */
class PostgresAiEvaluationStore(private val db: Database) : AiEvaluationStore {

    init {
        transaction(db) {
            // addLogger(StdOutSqlLogger)
            SchemaUtils.create(PGAiEvaluations)
        }
    }

    override fun recordFor(key: AiEvaluationKey) = transaction(db) {
        return@transaction select(key)
    }

    override fun insertIfAbsent(record: AiEvaluationRecord) = transaction(db) {
        PGAiEvaluations.insertIgnore {
            it[revision] = record.key.revision
            it[definitionFingerprint] = record.key.definitionFingerprint
            it[inputHash] = record.key.inputHash
            it[value] = record.value
            it[evidence] = record.evidence
            it[definition] = expressionToString(record.definition)
            it[evaluatedAt] = record.evaluatedAt
            it[requestedModel] = record.requestedModel
            it[reportedModel] = record.reportedModel
        }
        return@transaction checkNotNull(select(record.key)) {
            "No evaluation record found for ${record.key.asString()} after inserting it."
        }
    }

    private fun select(key: AiEvaluationKey): AiEvaluationRecord? =
        PGAiEvaluations.selectAll()
            .where {
                (PGAiEvaluations.revision eq key.revision) and
                        (PGAiEvaluations.definitionFingerprint eq key.definitionFingerprint) and
                        (PGAiEvaluations.inputHash eq key.inputHash)
            }
            .map { it.toRecord() }
            .firstOrNull()

    private fun ResultRow.toRecord() = AiEvaluationRecord(
        key = AiEvaluationKey(
            revision = this[PGAiEvaluations.revision],
            definitionFingerprint = this[PGAiEvaluations.definitionFingerprint],
            inputHash = this[PGAiEvaluations.inputHash]
        ),
        value = this[PGAiEvaluations.value],
        evidence = this[PGAiEvaluations.evidence],
        definition = expressionFromString(this[PGAiEvaluations.definition]) as AiBoolean,
        evaluatedAt = this[PGAiEvaluations.evaluatedAt],
        requestedModel = this[PGAiEvaluations.requestedModel],
        reportedModel = this[PGAiEvaluations.reportedModel]
    )
}

object PGAiEvaluations : IntIdTable(name = AI_EVALUATIONS_TABLE) {
    val revision = varchar("revision", 128)
    val definitionFingerprint = varchar("definition_fingerprint", 64)
    val inputHash = varchar("input_hash", 64)
    val value = bool("value")
    val evidence = text("evidence").nullable()
    val definition = text("definition")
    val evaluatedAt = long("evaluated_at")
    val requestedModel = varchar("requested_model", 128)
    val reportedModel = varchar("reported_model", 128).nullable()

    init {
        uniqueIndex("ai_evaluations_key", revision, definitionFingerprint, inputHash)
    }
}
