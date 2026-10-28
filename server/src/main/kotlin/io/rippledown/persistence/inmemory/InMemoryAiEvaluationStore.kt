package io.rippledown.persistence.inmemory

import io.rippledown.model.rule.evaluation.AiEvaluationKey
import io.rippledown.model.rule.evaluation.AiEvaluationRecord
import io.rippledown.persistence.AiEvaluationStore
import java.util.concurrent.ConcurrentHashMap

class InMemoryAiEvaluationStore : AiEvaluationStore {
    private val records = ConcurrentHashMap<AiEvaluationKey, AiEvaluationRecord>()

    override fun all() = records.values.toList()

    override fun recordFor(key: AiEvaluationKey) = records[key]

    override fun insertIfAbsent(record: AiEvaluationRecord): AiEvaluationRecord =
        records.putIfAbsent(record.key, record) ?: record
}
