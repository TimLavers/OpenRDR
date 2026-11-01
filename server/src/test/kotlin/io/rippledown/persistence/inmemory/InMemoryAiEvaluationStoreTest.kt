package io.rippledown.persistence.inmemory

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.model.Attribute
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.AiBooleanExample
import io.rippledown.model.rule.AiEvaluationProfile
import io.rippledown.model.rule.evaluation.AiEvaluationRecord
import io.rippledown.model.rule.evaluation.AiEvaluationRequest
import io.rippledown.model.rule.evaluation.key
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test

class InMemoryAiEvaluationStoreTest {
    private val hasDiabetes = AiBoolean(
        sourceAttribute = Attribute(3, "clinical_notes"),
        instructions = "True if the notes affirm a personal history of diabetes.",
        examples = listOf(AiBooleanExample("FH diab", false, "Family history only")),
        revision = "rev-1",
        evaluationProfile = AiEvaluationProfile("gemini", "gemini-2.5-flash", 0.0, 1, 1)
    )
    private val key = AiEvaluationRequest(hasDiabetes, "T2 Diab").key()
    private val record = AiEvaluationRecord(
        key = key,
        value = true,
        evidence = "T2 Diab",
        definition = hasDiabetes,
        evaluatedAt = 1_700_000_000_000,
        requestedModel = "gemini-2.5-flash",
        reportedModel = "gemini-2.5-flash-001"
    )
    private lateinit var store: InMemoryAiEvaluationStore

    @BeforeTest
    fun setup() {
        store = InMemoryAiEvaluationStore()
    }

    @Test
    fun `initially holds nothing`() {
        store.recordFor(key).shouldBeNull()
    }

    @Test
    fun `insert-if-absent stores and returns a new record`() {
        store.insertIfAbsent(record) shouldBeSameInstanceAs record
        store.recordFor(key) shouldBe record
    }

    @Test
    fun `a record without evidence is stored`() {
        val falseRecord = record.copy(value = false, evidence = null, reportedModel = null)
        store.insertIfAbsent(falseRecord) shouldBe falseRecord
        store.recordFor(key) shouldBe falseRecord
    }

    @Test
    fun `a conflicting duplicate cannot change the stored result`() {
        // Given a stored record
        store.insertIfAbsent(record)

        // When a different result for the same key is inserted
        val conflicting = record.copy(value = false, evidence = null, evaluatedAt = 1_700_000_001_000)
        val winner = store.insertIfAbsent(conflicting)

        // Then the first record is the winner and remains stored
        winner shouldBe record
        store.recordFor(key) shouldBe record
    }

    @Test
    fun `records for different keys are independent`() {
        // Given records for two inputs
        val other = AiEvaluationRequest(hasDiabetes, "FH diab").key()
        val otherRecord = record.copy(key = other, value = false, evidence = null)
        store.insertIfAbsent(record)
        store.insertIfAbsent(otherRecord)

        // Then each key returns its own record
        store.recordFor(key) shouldBe record
        store.recordFor(other) shouldBe otherRecord
    }

    @Test
    fun `concurrent duplicates agree on one winner`() {
        // Given many threads inserting different results for the same key at once
        val threads = 16
        val executor = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val candidates = (0 until threads).map { record.copy(value = it % 2 == 0, evaluatedAt = it.toLong()) }

        // When they all race
        val winners = candidates.map { candidate ->
            executor.submit<AiEvaluationRecord> {
                start.await()
                store.insertIfAbsent(candidate)
            }
        }
        start.countDown()
        val results = winners.map { it.get(5, TimeUnit.SECONDS) }
        executor.shutdown()

        // Then every caller received the same record, which is the one stored
        results.toSet().size shouldBe 1
        store.recordFor(key) shouldBe results.first()
    }
}
