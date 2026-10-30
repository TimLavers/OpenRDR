package io.rippledown.server

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.kb.KB
import io.rippledown.kb.KBSession
import io.rippledown.kb.chat.OpenKnowledgeBases
import io.rippledown.model.KBInfo
import io.rippledown.model.PresentUser
import io.rippledown.model.UserId
import io.rippledown.model.change.ChangeKind
import io.rippledown.persistence.inmemory.InMemoryKB
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ApplicationKbAwarenessTest {
    private val alice = UserId("Alice")
    private val bob = UserId("Bob")
    private val kb = KB(InMemoryKB(KBInfo("clinic", "Clinic")))
    private val endpoint = KBEndpoint(KBSession(kb))
    private val open = mockk<OpenKnowledgeBases>()
    private val presence = mockk<Presence>()
    private val app = mockk<ServerApplication>()
    private val service = ApplicationKbService(app, mockk(), alice, open)

    @BeforeEach
    fun setUp() {
        every { open.openEndpointFor(alice) } returns endpoint
        every { app.presence } returns presence
    }

    private fun record(user: UserId = bob, attributeIds: Set<Int> = emptySet()) =
        kb.changeLog.record(user, ChangeKind.RULES_ADDED, "Added a rule.", attributeIds = attributeIds)

    @Test
    fun `recent changes include all authors in newest first order and obey the limit`() {
        // Given
        val first = record()
        val second = record(alice)
        val third = record()

        // When
        val recent = service.recentChanges(2, null)

        // Then
        recent shouldBe listOf(third, second)
        service.recentChanges(1, null) shouldBe listOf(third)
        service.recentChanges(Int.MAX_VALUE, null) shouldBe listOf(third, second, first)
        kb.changeLog.all() shouldBe listOf(first, second, third)
    }

    @Test
    fun `an empty history returns no records`() {
        // Given an empty KB

        // When
        val recent = service.recentChanges(10, null)

        // Then
        recent shouldBe emptyList()
    }

    @Test
    fun `attribute filtering precedes the limit and resolves normalized exact names`() {
        // Given
        val bmi = kb.attributeManager.getOrCreate("Body Mass Index")
        record(attributeIds = setOf(bmi.id))
        val second = record(attributeIds = setOf(bmi.id))
        val third = record(attributeIds = setOf(bmi.id))
        record()

        // When
        val recent = service.recentChanges(2, "  'body   MASS index'  ")

        // Then
        recent shouldBe listOf(third, second)
    }

    @Test
    fun `history follows a renamed attribute by its id`() {
        // Given
        val bmi = kb.attributeManager.getOrCreate("BMI")
        val change = record(attributeIds = setOf(bmi.id))
        kb.attributeManager.rename(bmi, "Body mass index")

        // When
        val recent = service.recentChanges(10, "Body mass index")

        // Then
        recent shouldBe listOf(change)
        shouldThrow<IllegalArgumentException> { service.recentChanges(10, "BMI") }.message shouldBe
                "No attribute named \"BMI\" exists."
    }

    @ParameterizedTest
    @ValueSource(strings = ["BIM", "BM", " "])
    fun `unknown or partial attribute names are refused without creating attributes`(name: String) {
        // Given
        val bmi = kb.attributeManager.getOrCreate("BMI")
        record(attributeIds = setOf(bmi.id))

        // When
        val failure = shouldThrow<IllegalArgumentException> { service.recentChanges(10, name) }

        // Then
        failure.message shouldBe "No attribute named \"$name\" exists."
        kb.attributeManager.all() shouldBe setOf(bmi)
    }

    @Test
    fun `a known attribute can have no recorded changes`() {
        // Given
        kb.attributeManager.getOrCreate("BMI")
        record()

        // When
        val recent = service.recentChanges(10, "BMI")

        // Then
        recent shouldBe emptyList()
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `nonpositive history limits are refused`(limit: Int) {
        // Given an open KB

        // When
        val failure = shouldThrow<IllegalArgumentException> { service.recentChanges(limit, null) }

        // Then
        failure.message shouldBe "Please request at least one change."
    }

    @Test
    fun `awareness queries require an open KB`() {
        // Given
        every { open.openEndpointFor(alice) } returns null

        // When / Then
        shouldThrow<IllegalStateException> { service.recentChanges(10, null) }.message shouldBe "No knowledge base is open."
        shouldThrow<IllegalStateException> { service.usersPresent() }.message shouldBe "No knowledge base is open."
        verify(exactly = 0) { presence.present(any()) }
    }

    @Test
    fun `presence excludes the caller and preserves other users editing flags`() {
        // Given
        every { presence.present("clinic") } returns listOf(
            PresentUser("Alice", true), PresentUser("Bob", true), PresentUser("Carol", false)
        )

        // When
        val users = service.usersPresent()

        // Then
        users shouldBe listOf(PresentUser("Bob", true), PresentUser("Carol", false))
        verify(exactly = 1) { presence.present("clinic") }
    }

    @Test
    fun `presence is empty when only the caller is connected`() {
        // Given
        every { presence.present("clinic") } returns listOf(PresentUser("Alice", false))

        // When
        val users = service.usersPresent()

        // Then
        users shouldBe emptyList()
    }

    @Test
    fun `queries follow the caller's open KB when it changes`() {
        // Given
        record()
        val other = KBEndpoint(KBSession(KB(InMemoryKB(KBInfo("other", "Other")))))
        every { open.openEndpointFor(alice) } returns other
        every { presence.present("other") } returns listOf(PresentUser("Carol", false))

        // When
        val history = service.recentChanges(10, null)
        val users = service.usersPresent()

        // Then
        history shouldBe emptyList()
        users shouldBe listOf(PresentUser("Carol", false))
        verify(exactly = 0) { presence.present("clinic") }
    }
}
