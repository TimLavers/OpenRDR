package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.startWith
import io.rippledown.model.KBInfo
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
        val info1 = kbManager.createKB(name, true)
        val info2 = kbManager.createKB(name, true)
        info1.name shouldBe name
        info2.name shouldBe name
        info1.id shouldNotBe info2.id
        kbManager.all() shouldContain info1
        kbManager.all() shouldContain info2
    }

    @Test //KBM-2
    fun `create two KBs with the same name, modulo case, requires force field`() {
        val name = "Snarky Puppy"
        val info1 = kbManager.createKB(name)
        shouldThrow<IllegalArgumentException> {
            kbManager.createKB(name.lowercase())
        }.message shouldBe "A KB with name Snarky Puppy already exists. Use force=true to create a KB with the same name, ignoring case, as an existing KB."
        kbManager.all() shouldContain info1
        kbManager.all().size shouldBe 1
    }

    @Test //KBM-5
    fun deleteKB() {
        val info1 = kbManager.createKB("Thyroids",)
        val info2 = kbManager.createKB("Glucose",)
        val info3 = kbManager.createKB("Lipids",)

        kbManager.all() shouldBe setOf(info1, info2, info3)
        kbManager.deleteKB(info2)
        kbManager.all() shouldBe setOf(info1, info3)
        kbManager.deleteKB(info3)
        kbManager.all() shouldBe setOf(info1)
        kbManager.deleteKB(info1)
        kbManager.all() shouldBe setOf()
    }

    @Test //KBM-5
    fun `delete non-existent KB`() {
        val info = KBInfo("Unknown")
        shouldThrow<IllegalArgumentException> {
            kbManager.deleteKB(info)
        }.message shouldBe "No KB with id $info was found."
    }

    @Test //KBM-5
    fun `delete a KB that has the same name as another KB`() {
        val name = "Snarky Puppy"
        val info1 = kbManager.createKB(name, true)
        val info2 = kbManager.createKB(name, true)
        kbManager.all() shouldBe setOf(info1, info2) // sanity
        kbManager.deleteKB(info2)
        kbManager.all() shouldBe setOf(info1)
    }

    @Test //KBM-5
    fun `delete a KB and then re-create it`() {
        val name = "Snarky Puppy"
        val info1 = kbManager.createKB(name,)
        kbManager.all() shouldBe setOf(info1)
        kbManager.deleteKB(info1)
        kbManager.all() shouldBe setOf()
        val info2 = kbManager.createKB(name)
        kbManager.all() shouldBe setOf(info2)
    }

    @Test //KBM-5
    fun `delete a KB that has the same id as another KB`() {
        val info1 = kbManager.createKB("Thyroids",)
        val info2 = kbManager.createKB("Glucose",)
        kbManager.all() shouldBe setOf(info1, info2) // sanity
        kbManager.deleteKB(KBInfo(info2.id, info1.name)) // Should never happen, but anyway...
        kbManager.all() shouldBe setOf(info1)
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