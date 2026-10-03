package io.rippledown.kb.lease

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.rippledown.model.UserId
import kotlin.test.Test

class ProjectLeaseTest {
    private val alice = UserId("alice")
    private val bob = UserId("bob")
    private var now = 1_000_000L
    private val lease = ProjectLease({ "Thyroids" }) { now }

    @Test
    fun `an unheld lease has no holder`() {
        // Then
        lease.holder().shouldBeNull()
    }

    @Test
    fun `the first hold takes the lease`() {
        // When
        val previous = lease.hold(alice)

        // Then
        previous.shouldBeNull()
        lease.holder() shouldBe alice
    }

    @Test
    fun `the holder can hold again`() {
        // Given
        lease.hold(alice)

        // When
        val previous = lease.hold(alice)

        // Then
        previous.shouldBeNull()
        lease.holder() shouldBe alice
    }

    @Test
    fun `another user is refused while the lease is held`() {
        // Given
        lease.hold(alice)

        // When
        val refusal = shouldThrow<ProjectHeldException> { lease.hold(bob) }

        // Then
        refusal.message shouldBe "Thyroids is being edited by alice."
        refusal.kbName shouldBe "Thyroids"
        refusal.holder shouldBe alice
        lease.holder() shouldBe alice
    }

    @Test
    fun `the refusal names the KB as it is now`() {
        // Given
        var name = "Thyroids"
        val renamed = ProjectLease({ name }) { now }
        renamed.hold(alice)
        name = "Thyroid Function"

        // When
        val refusal = shouldThrow<ProjectHeldException> { renamed.hold(bob) }

        // Then
        refusal.message shouldBe "Thyroid Function is being edited by alice."
    }

    @Test
    fun `another user is refused just before expiry`() {
        // Given
        lease.hold(alice)
        now += LEASE_EXPIRY_MS - 1

        // When / Then
        shouldThrow<ProjectHeldException> { lease.hold(bob) }
    }

    @Test
    fun `another user takes an expired lease and is told who lost it`() {
        // Given
        lease.hold(alice)
        now += LEASE_EXPIRY_MS

        // When
        val previous = lease.hold(bob)

        // Then
        previous shouldBe alice
        lease.holder() shouldBe bob
    }

    @Test
    fun `each hold by the holder renews the lease`() {
        // Given
        lease.hold(alice)
        now += LEASE_EXPIRY_MS - 1
        lease.hold(alice)
        now += LEASE_EXPIRY_MS - 1

        // When / Then
        shouldThrow<ProjectHeldException> { lease.hold(bob) }
    }

    @Test
    fun `an expired lease still reports its holder until someone takes it`() {
        // Given
        lease.hold(alice)
        now += LEASE_EXPIRY_MS

        // Then
        lease.holder() shouldBe alice
    }

    @Test
    fun `release frees the lease`() {
        // Given
        lease.hold(alice)

        // When
        val released = lease.release()

        // Then
        released shouldBe alice
        lease.holder().shouldBeNull()
        lease.hold(bob).shouldBeNull()
    }

    @Test
    fun `releasing an unheld lease is a no-op`() {
        // When / Then
        lease.release().shouldBeNull()
    }

    @Test
    fun `releaseIfHeldBy releases only the holder's lease`() {
        // Given
        lease.hold(alice)

        // When
        val byBob = lease.releaseIfHeldBy(bob)
        val byAlice = lease.releaseIfHeldBy(alice)

        // Then
        byBob shouldBe false
        byAlice shouldBe true
        lease.holder().shouldBeNull()
    }

    @Test
    fun `the expiry is ten minutes`() {
        // Then
        LEASE_EXPIRY_MS shouldBe 10 * 60 * 1000L
    }
}
