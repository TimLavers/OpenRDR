package io.rippledown.server

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.model.AttributeKind
import io.rippledown.model.KBInfo
import io.rippledown.model.Result
import io.rippledown.model.diff.Addition
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.external.MeasurementEvent
import io.rippledown.model.rule.BuildRuleRequest
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.concurrent.thread
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * The lab feed and a user edit the one KB from different threads. Without the
 * per-KB lock this corrupts the shared object graph (duplicate attributes,
 * ConcurrentModificationException while a case is interpreted during a commit).
 * See documentation/design/concurrent_users_write_lock.md.
 */
class KBEndpointConcurrencyTest {
    private lateinit var kb: KB
    private lateinit var endpoint: KBEndpoint

    private val ingestingThreads = 4
    private val casesPerThread = 50

    @BeforeTest
    fun setup() {
        kb = KB(InMemoryKB(KBInfo("id123", "Stress")))
        endpoint = KBEndpoint(KBSession(kb))
    }

    private fun externalCase(name: String, vararg attributeToValue: Pair<String, String>) =
        ExternalCase(name, attributeToValue.associate { MeasurementEvent(it.first, defaultDate) to Result(it.second) })

    @Test
    fun `cases ingested while a user edits the KB leave it consistent`() {
        // Given a KB with one case, so that a rule can be built on it
        endpoint.processCase(externalCase("Seed", "Glucose" to "12.0", "Shared" to "1"))
        val failures = ConcurrentLinkedQueue<Throwable>()
        val start = CountDownLatch(1)

        // When several lab threads post cases carrying new and shared attribute names
        val ingesters = (1..ingestingThreads).map { t ->
            thread {
                start.await(5, SECONDS)
                repeat(casesPerThread) { i ->
                    try {
                        endpoint.processCase(
                            externalCase("Case $t-$i", "Glucose" to "$i", "Shared" to "$i", "New $t-$i" to "x")
                        )
                    } catch (e: Throwable) {
                        failures.add(e)
                    }
                }
            }
        }
        // and a user reorders attributes and builds rules at the same time
        val editor = thread {
            start.await(5, SECONDS)
            repeat(20) { i ->
                try {
                    val attributes = endpoint.session.locked { kb.caseViewManager.allInOrder() }
                    if (attributes.size >= 2) endpoint.moveAttribute(attributes.last().id, attributes.first().id)
                    endpoint.buildRule(
                        BuildRuleRequest("Seed", Addition("Comment $i."), listOf("""Shared is "1""""))
                    )
                } catch (e: Throwable) {
                    failures.add(e)
                }
            }
        }
        start.countDown()
        (ingesters + editor).forEach { it.join(60_000) }

        // Then nothing threw, and the KB has exactly the attributes the cases named
        failures.toList().shouldBeEmpty()
        val expectedExternalNames = setOf("Glucose", "Shared") +
                (1..ingestingThreads).flatMap { t -> (0 until casesPerThread).map { i -> "New $t-$i" } }
        kb.attributeManager.all().filter { it.kind == AttributeKind.EXTERNAL }.map { it.name }.toSet() shouldBe
                expectedExternalNames
        kb.allProcessedCases().size shouldBe 1 + ingestingThreads * casesPerThread
    }
}
