package io.rippledown.kb

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.startWith
import io.rippledown.persistence.InMemoryPersistenceProvider
import io.rippledown.util.EntityRetrieval
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class KBManagerTest {
    private lateinit var kbManager: KBManager
    private lateinit var persistenceProvider: InMemoryPersistenceProvider

    @BeforeTest
    fun setup() {
        persistenceProvider = InMemoryPersistenceProvider()
        kbManager = KBManager(persistenceProvider)
    }

    @Test //KBM-1
    fun empty() {
        kbManager.all() shouldBe emptySet()
    }

    @Test //KBM-2, KBM-3
    fun createKB() {
        val name = "Snarky Puppy"
        val info = kbManager.createKB(name)
        info.name shouldBe name
        // Check that the id is valid //KBM-3
        info.id should startWith("snarkypuppy_")
        kbManager.all() shouldContain info
        // Check that the kb can be retrieved.
        val retrievedKB = (kbManager.openKB(info.id) as EntityRetrieval.Success).entity
        retrievedKB.kbInfo shouldBe info

        // Rebuild the KBManager.
        kbManager = KBManager(persistenceProvider)
        kbManager.all() shouldContain info
        // Check that the kb can be retrieved.
        val retrievedKBAfterRebuild = (kbManager.openKB(info.id) as EntityRetrieval.Success).entity
        retrievedKBAfterRebuild.kbInfo shouldBe info
    }

    @Test //KBM-2
    fun `create two KBs with the same name`() {
        val name = "Snarky Puppy"
        val info1 = kbManager.createKB(name)
        val info2 = kbManager.createKB(name)
        info1.name shouldBe name
        info2.name shouldBe name
        info1.id shouldNotBe info2.id
        kbManager.all() shouldContain info1
        kbManager.all() shouldContain info2
    }

    @Test //KBM-4
    fun openKB() {
        add10KBs()
        val name = "Snarky Puppy"
        val info = kbManager.createKB(name)

        val success = kbManager.openKB(info.id) as EntityRetrieval.Success
        success.entity.kbInfo shouldBe info
    }

    @Test //KBM-4
    fun `id unknown in openKB call`() {
        add10KBs()
        val id = UUID.randomUUID().toString()
        val failure = kbManager.openKB(id) as EntityRetrieval.Failure
        failure.errorMessage shouldBe "Unknown id: $id."
    }

    private fun add10KBs() {
        repeat(10) {
            kbManager.createKB("kb$it")
        }
    }
}