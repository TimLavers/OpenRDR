package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.startWith
import io.rippledown.constants.chat.kbNameReservedMessage
import io.rippledown.model.KBInfo
import io.rippledown.persistence.inmemory.InMemoryPersistenceProvider
import io.rippledown.sample.SampleKB
import io.rippledown.util.EntityRetrieval
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.*
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

    @ParameterizedTest
    @EnumSource(SampleKB::class, names = ["TSH", "CONTACT_LENSES", "ZOO", "PATHOLOGY"])
    fun `reserved titles cannot be created even with force`(sample: SampleKB) {
        // Given
        val names = listOf(sample.title(), " ${sample.title().lowercase()} ")
        for (name in names) for (force in listOf(false, true)) {
            // When
            val error = shouldThrow<IllegalArgumentException> { kbManager.createKB(name, force) }

            // Then
            error.message shouldBe kbNameReservedMessage(name.trim())
            kbManager.all() shouldBe emptySet()
            persistenceProvider.idStore().data() shouldBe emptyMap()
        }
    }

    @ParameterizedTest
    @EnumSource(SampleKB::class, names = ["TSH", "CONTACT_LENSES", "ZOO", "PATHOLOGY"])
    fun `reserved rename leaves the name unchanged in memory and persistence`(sample: SampleKB) {
        // Given
        val original = kbManager.createKB("MyCopy")
        val kb = (kbManager.openKB(original.id) as EntityRetrieval.Success).entity
        for (name in listOf(sample.title(), " ${sample.title().lowercase()} ")) {
            // When
            val error = shouldThrow<IllegalArgumentException> { kbManager.renameKB(original.id, name) }

            // Then
            error.message shouldBe kbNameReservedMessage(name.trim())
            kb.kbInfo.name shouldBe "MyCopy"
            kbManager.all().map { it.name } shouldBe listOf("MyCopy")
            persistenceProvider.kbPersistence(original.id).kbInfo().name shouldBe "MyCopy"
        }
    }

    @Test
    fun `a registered KB is listed and opened as the same instance`() {
        // Given
        val kbInfo = KBInfo("Imported")
        val kb = KB(persistenceProvider.createKBPersistence(kbInfo))

        // When
        kbManager.register(kb)

        // Then
        kbManager.all() shouldBe setOf(kbInfo)
        (kbManager.openKB(kbInfo.id) as EntityRetrieval.Success).entity shouldBe kb
        kbManager.renameKB(kbInfo.id, "Renamed").name shouldBe "Renamed"
        kb.kbInfo.name shouldBe "Renamed"
    }

    @Test
    fun `registering a KB with a known id replaces the previous info and instance`() {
        // Given
        val original = kbManager.createKB("Thyroids")
        kbManager.renameKB(original.id, "Thyroid Function")
        val reimported = KB(persistenceProvider.createKBPersistence(KBInfo(original.id, "Thyroids")))

        // When
        kbManager.register(reimported)

        // Then
        kbManager.all().map { it.name } shouldBe listOf("Thyroids")
        (kbManager.openKB(original.id) as EntityRetrieval.Success).entity shouldBe reimported
    }

    @Test
    fun `a legacy KB with a reserved name can still be opened and renamed`() {
        // Given
        val legacy = KBInfo("legacy_id", "Pathology")
        persistenceProvider.createKBPersistence(legacy)
        kbManager = KBManager(persistenceProvider)

        // When
        val kb = (kbManager.openKB(legacy.id) as EntityRetrieval.Success).entity
        val renamed = kbManager.renameKB(legacy.id, "My Pathology")

        // Then
        renamed.id shouldBe legacy.id
        kb.kbInfo.name shouldBe "My Pathology"
        persistenceProvider.kbPersistence(legacy.id).kbInfo().name shouldBe "My Pathology"
    }

    @Test //KBM-1
    fun empty() {
        kbManager.all() shouldBe emptySet()
    }

    @Test
    fun `an unreadable stored KB does not prevent other KBs from loading`() {
        // Given
        persistenceProvider.idStore().add("unavailable", true)
        val readable = kbManager.createKB("MyCopy")

        // When
        val reloaded = KBManager(persistenceProvider)

        // Then
        reloaded.all() shouldBe setOf(readable)
        val kb = (reloaded.openKB(readable.id) as EntityRetrieval.Success).entity
        kb.kbInfo.name shouldBe "MyCopy"
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
        val info1 = kbManager.createKB("Thyroids")
        val info2 = kbManager.createKB("Glucose")
        val info3 = kbManager.createKB("Lipids")

        kbManager.all() shouldBe setOf(info1, info2, info3)
        kbManager.deleteKB(info2)
        kbManager.all() shouldBe setOf(info1, info3)
        kbManager.deleteKB(info3)
        kbManager.all() shouldBe setOf(info1)
        kbManager.deleteKB(info1)
        kbManager.all() shouldBe setOf()
    }

    @Test //KBM-5
    fun `deleting a KB should return the KBInfo of the remaining KB first in alpha order`() {
        //Given
        val info1 = kbManager.createKB("Thyroids")
        val info2 = kbManager.createKB("Glucose")
        val info3 = kbManager.createKB("Lipids")

        //When
        val kbInfo = kbManager.deleteKB(info2)

        //Then
        kbInfo shouldBe info3

        //And when the last KB in alpha order is deleted
        val remaining = kbManager.deleteKB(info1)

        //Then the first remaining KB in alpha order is returned
        remaining shouldBe info3
    }

    @Test //KBM-5
    fun `deleting the only KB should return null`() {
        //Given
        val info = kbManager.createKB("Thyroids")

        //When
        val kbInfo = kbManager.deleteKB(info)

        //Then
        kbInfo shouldBe null
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
        val info1 = kbManager.createKB(name)
        kbManager.all() shouldBe setOf(info1)
        kbManager.deleteKB(info1)
        kbManager.all() shouldBe setOf()
        val info2 = kbManager.createKB(name)
        kbManager.all() shouldBe setOf(info2)
    }

    @Test //KBM-5
    fun `delete a KB that has the same id as another KB`() {
        val info1 = kbManager.createKB("Thyroids")
        val info2 = kbManager.createKB("Glucose")
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

    @Test //KBM-7
    fun `rename a KB preserves its id and persists the new name`() {
        // given
        val original = kbManager.createKB("Thyroids")

        // when
        val renamed = kbManager.renameKB(original.id, "Thyroid Function")

        // then
        renamed shouldBe KBInfo(original.id, "Thyroid Function")
        kbManager.all() shouldBe setOf(renamed)
        kbManager = KBManager(persistenceProvider)
        kbManager.all() shouldBe setOf(renamed)
        val reopened = (kbManager.openKB(original.id) as EntityRetrieval.Success).entity
        reopened.kbInfo.name shouldBe "Thyroid Function"
    }

    @Test //KBM-7
    fun `rename refuses a name already used by another KB ignoring case`() {
        // given
        val thyroids = kbManager.createKB("Thyroids")
        val glucose = kbManager.createKB("Glucose")

        // when
        val error = shouldThrow<IllegalArgumentException> {
            kbManager.renameKB(thyroids.id, "glucose")
        }

        // then
        error.message shouldBe "A KB with name Glucose already exists."
        kbManager.all() shouldBe setOf(thyroids, glucose)
    }

    @Test //KBM-7
    fun `rename refuses an unknown KB id`() {
        // given
        val unknownId = "unknown_1"

        // when
        val error = shouldThrow<IllegalArgumentException> {
            kbManager.renameKB(unknownId, "New name")
        }

        // then
        error.message shouldBe "No KB with id $unknownId was found."
    }

    @Test //KBM-7
    fun `rename validates the new name before changing persistence`() {
        // given
        val original = kbManager.createKB("Thyroids")

        // when
        shouldThrow<IllegalArgumentException> {
            kbManager.renameKB(original.id, "")
        }

        // then
        kbManager.all() shouldBe setOf(original)
        persistenceProvider.kbPersistence(original.id).kbInfo() shouldBe original
    }

    private fun add10KBs() {
        repeat(10) {
            kbManager.createKB("kb$it")
        }
    }
}
