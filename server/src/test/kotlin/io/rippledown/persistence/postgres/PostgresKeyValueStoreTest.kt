package io.rippledown.persistence.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.persistence.KeyValue
import io.rippledown.persistence.KeyValueStore
import org.jetbrains.exposed.dao.exceptions.EntityNotFoundException
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresKeyValueStoreTest: PostgresStoreTest() {
    private lateinit var store: KeyValueStore

    override fun tablesInDropOrder() = listOf(META_DATA_STORE)

    override fun reload() {
        super.reload()
        store = postgresKB.metaDataStore()
    }

    @BeforeTest
    fun setup() {
        dropTable()
        store = postgresKB.metaDataStore()
    }

    @Test
    fun `initially empty`() {
        store.all() shouldBe emptySet()
    }

    @Test
    fun `should throw exception if asked to create an item with an existing key`() {
        with(store) {
            val beach = create("beach", "Bondi")
            shouldThrow<IllegalArgumentException> {
                create(beach.key, "Bronte")
            }
        }
    }

    @Test
    fun `create and retrieve`() {
        with(store) {
            val beach = create("beach", "Bondi")
            beach.key shouldBe "beach"
            beach.value shouldBe "Bondi"
            val walk = create("bushwalk", "Bundanoon")
            walk.key shouldBe "bushwalk"
            walk.value shouldBe "Bundanoon"
            walk.id shouldNotBe beach.id
            val gallery = create("gallery", "Canberra")
            gallery.key shouldBe "gallery"
            gallery.value shouldBe "Canberra"
            gallery.id shouldNotBe beach.id
            gallery.id shouldNotBe walk.id
            with(all()) {
                size shouldBe 3
                contains(beach) shouldBe true
                contains(walk) shouldBe true
                contains(gallery) shouldBe true
            }
        }
    }

    @Test
    fun `for update, then if either key or value match an existing item, then both should match`(){
        with(store) {
            val beach = create("beach", "Bondi")
            shouldThrow<IllegalArgumentException> {
                store(KeyValue(beach.id, "walk", "Bundo"))
            }
            shouldThrow<EntityNotFoundException> {
                store(KeyValue(beach.id + 1, "beach", "Northgong"))
            }
        }
    }

    @Test
    fun `for update, key and id should match those of an existing item`(){
        with(store) {
            val beach = create("beach", "Bondi")
            shouldThrow<IllegalArgumentException> {
                store(KeyValue(beach.id + 1, "walk", "Bundo"))
            }
        }
    }

    @Test
    fun `create and then update`() {
        with(store) {
            val beach = create("beach", "Bondi")
            store(beach.copy(value = "Bronte"))
            with (all()){
                size shouldBe 1
                first().id shouldBe beach.id
                first().key shouldBe beach.key
                first().value shouldBe "Bronte"
            }
        }
    }

    @Test
    fun `cannot load if there are already existing items`() {
        with(store) {
            create("beach", "Bondi")
            shouldThrow<IllegalArgumentException> {
                load(HashSet())
            }.message shouldBe "Cannot load key/value items if there are are some stored already."
        }
    }
}