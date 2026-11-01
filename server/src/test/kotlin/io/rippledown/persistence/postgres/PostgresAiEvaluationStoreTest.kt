package io.rippledown.persistence.postgres

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.rule.AiBoolean
import io.rippledown.model.rule.AiBooleanExample
import io.rippledown.model.rule.AiEvaluationProfile
import io.rippledown.model.rule.evaluation.AiEvaluationRecord
import io.rippledown.model.rule.evaluation.AiEvaluationRequest
import io.rippledown.model.rule.evaluation.key
import io.rippledown.persistence.AiEvaluationStore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresAiEvaluationStoreTest : PostgresStoreTest() {
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
    private lateinit var store: AiEvaluationStore

    @BeforeTest
    fun setup() {
        clearTables()
        store = postgresKB.aiEvaluationStore()
    }

    override fun reload() {
        super.reload()
        store = postgresKB.aiEvaluationStore()
    }

    @Test
    fun `initially holds nothing`() {
        store.recordFor(key).shouldBeNull()
    }

    @Test
    fun `a record survives a reload`() {
        // When a record is inserted
        store.insertIfAbsent(record) shouldBe record

        // Then it is read back unchanged, including after the store is rebuilt from the database
        store.recordFor(key) shouldBe record
        reload()
        store.recordFor(key) shouldBe record
    }

    @Test
    fun `a record without evidence or reported model survives a reload`() {
        val falseRecord = record.copy(value = false, evidence = null, reportedModel = null)
        store.insertIfAbsent(falseRecord) shouldBe falseRecord
        reload()
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
        reload()
        store.recordFor(key) shouldBe record
    }

    @Test
    fun `records for different keys are independent`() {
        val other = AiEvaluationRequest(hasDiabetes, "FH diab").key()
        val otherRecord = record.copy(key = other, value = false, evidence = null)
        store.insertIfAbsent(record)
        store.insertIfAbsent(otherRecord)

        store.recordFor(key) shouldBe record
        store.recordFor(other) shouldBe otherRecord
    }

    @Test
    fun `the same definition under a new revision has its own record`() {
        val revised = AiEvaluationRequest(hasDiabetes.copy(revision = "rev-2"), "T2 Diab").key()
        store.insertIfAbsent(record)

        store.recordFor(revised).shouldBeNull()
    }

    @Test
    fun `concurrent duplicates agree on one winner`() {
        // Given several threads inserting different results for the same key at once
        val threads = 8
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
        val results = winners.map { it.get(30, TimeUnit.SECONDS) }
        executor.shutdown()

        // Then every caller received the same record, which is the one stored
        results.toSet().size shouldBe 1
        store.recordFor(key) shouldBe results.first()
    }
}
