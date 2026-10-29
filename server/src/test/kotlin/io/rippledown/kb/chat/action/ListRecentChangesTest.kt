package io.rippledown.kb.chat.action

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.constants.chat.NO_KB_OPEN_MESSAGE
import io.rippledown.kb.chat.KnowledgeBaseService
import io.rippledown.model.KBInfo
import io.rippledown.model.change.ChangeKind
import io.rippledown.model.change.ChangeRecord
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ListRecentChangesTest {
    private val service = mockk<KnowledgeBaseService>()
    private val clock = Clock.fixed(Instant.parse("2026-10-25T00:15:00Z"), ZoneId.of("Australia/Sydney"))

    @BeforeEach
    fun setUp() {
        every { service.openKnowledgeBase() } returns KBInfo("clinic", "Clinic")
    }

    private fun record(id: Int, timestamp: String, user: String, summary: String) =
        ChangeRecord(id, user, Instant.parse(timestamp).toEpochMilli(), ChangeKind.RULES_ADDED, summary)

    @Test
    fun `lists newest records first with users and local dates across midnight`() = runTest {
        // Given the service returns newest first, including a prior UTC date that is today locally
        val records = listOf(
            record(3, "2026-10-25T00:05:00Z", "Carol", "Added a rule."),
            record(2, "2026-10-24T14:05:00Z", "Bob", "Renamed BMI."),
            record(1, "2026-10-24T12:00:00Z", "Alice", "Set the description.")
        )
        every { service.recentChanges(10, null) } returns records

        // When
        val outcome = ListRecentChanges(clock = clock).doIt(service)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response.text shouldBe
                "11:05 — Carol: Added a rule.\n01:05 — Bob: Renamed BMI.\n24 Oct 23:00 — Alice: Set the description."
        verify(exactly = 1) { service.recentChanges(10, null) }
    }

    @Test
    fun `the same month and day in a different year is not today`() = runTest {
        // Given
        every { service.recentChanges(10, null) } returns listOf(record(1, "2025-10-25T00:05:00Z", "Bob", "Old change."))

        // When
        val outcome = ListRecentChanges(clock = clock).doIt(service)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response.text shouldBe "25 Oct 11:05 — Bob: Old change."
    }

    @Test
    fun `passes the requested limit and attribute to the service`() = runTest {
        // Given
        every { service.recentChanges(2, "BMI") } returns listOf(record(1, "2026-10-25T00:05:00Z", "Bob", "Changed BMI."))

        // When
        val outcome = ListRecentChanges(2, "BMI", clock).doIt(service)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response.text shouldBe "11:05 — Bob: Changed BMI."
        verify(exactly = 1) { service.recentChanges(2, "BMI") }
    }

    @Test
    fun `an empty history explains that nothing has been recorded`() = runTest {
        // Given
        every { service.recentChanges(10, null) } returns emptyList()

        // When
        val outcome = ListRecentChanges().doIt(service)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response.text shouldBe
                "No changes have been recorded for this knowledge base."
    }

    @Test
    fun `an attribute without changes has a distinct empty response`() = runTest {
        // Given
        every { service.recentChanges(10, "BMI") } returns emptyList()

        // When
        val outcome = ListRecentChanges(attribute = "BMI").doIt(service)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response.text shouldBe "No recorded changes concern \"BMI\"."
    }

    @Test
    fun `an unknown attribute is reported instead of an empty history`() = runTest {
        // Given
        every { service.recentChanges(10, "BIM") } throws IllegalArgumentException("No attribute named \"BIM\" exists.")

        // When
        val outcome = ListRecentChanges(attribute = "BIM").doIt(service)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response.text shouldBe "No attribute named \"BIM\" exists."
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, Int.MIN_VALUE])
    fun `a nonpositive limit is refused without querying history`(limit: Int) = runTest {
        // Given
        val action = ListRecentChanges(limit)

        // When
        val outcome = action.doIt(service)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response.text shouldBe "Please request at least one change."
        verify(exactly = 0) { service.recentChanges(any(), any()) }
    }

    @Test
    fun `without an open KB history is not queried`() = runTest {
        // Given
        every { service.openKnowledgeBase() } returns null

        // When
        val outcome = ListRecentChanges().doIt(service)

        // Then
        outcome.shouldBeInstanceOf<KbManagementOutcome.Done>().response.text shouldBe NO_KB_OPEN_MESSAGE
        verify(exactly = 0) { service.recentChanges(any(), any()) }
    }
}
