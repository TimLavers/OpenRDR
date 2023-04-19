package io.rippledown.persistence.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.Conclusion
import org.apache.commons.lang3.RandomStringUtils
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresConclusionStoreTest: PostgresStoreTest() {
    private val teaComment = "Time for a refreshing cup of tea!"
    private val coffeeComment = "Time for a revivifying cup of coffee!"
    private val wineComment = "Time for a strengthening glass of cab sav!"

    private lateinit var store: PostgresConclusionStore

    override fun tableName() = ATTRIBUTE_INDEXES_TABLE

    @BeforeTest
    fun setup() {
        dropTable()
        store = PostgresConclusionStore(dbName)
    }

    @Test
    fun all() {
        store.all() shouldBe emptySet()

        val conclusionsCreated = mutableSetOf<Conclusion>()
        repeat(100) {
            val conclusion = store.create("Conclusion $it")
            conclusionsCreated.add(conclusion)
            store.all() shouldBe conclusionsCreated
        }
        // Rebuild and check.
        store = PostgresConclusionStore(dbName)
        store.all() shouldBe conclusionsCreated
    }

    @Test
    fun `can create long conclusions`() {
        val longComment = RandomStringUtils.random(2048)
        val created = store.create(longComment)

        store.all() shouldBe setOf(created)

        // Rebuild and check.
        store = PostgresConclusionStore(dbName)
        store.all() shouldBe setOf(created)
    }

    @Test
    fun create() {
        val teaConclusion = store.create(teaComment)
        teaConclusion.text shouldBe teaComment

        val coffeeConclusion = store.create(coffeeComment)
        coffeeConclusion.text shouldBe coffeeComment

        store.all() shouldBe setOf(teaConclusion, coffeeConclusion)

        // Rebuild and check.
        store = PostgresConclusionStore(dbName)
        store.all() shouldBe setOf(teaConclusion, coffeeConclusion)
    }

    @Test
    fun `cannot create conclusion with text equal to that of existing conclusion`() {
        store.create(teaComment)
        store.create(coffeeComment)
        store.create(wineComment)

        shouldThrow<IllegalArgumentException> {
            store.create(coffeeComment)
        }.message shouldBe "A conclusion with the given text already exists."
    }

    @Test
    fun store() {
        val teaConclusion = store.create(teaComment)
        getById(teaConclusion.id).text shouldBe teaComment
        store.create(coffeeComment)
        store.create(wineComment)

        store.store(teaConclusion)
        getById(teaConclusion.id).text shouldBe teaComment

        store.store(Conclusion(teaConclusion.id, wineComment))
        getById(teaConclusion.id).text shouldBe wineComment

        // Rebuild and check.
        store = PostgresConclusionStore(dbName)
        getById(teaConclusion.id).text shouldBe wineComment
    }

    @Test
    fun `load not allowed if non-empty`() {
        store.create(teaComment)

        shouldThrow<IllegalArgumentException> {
            store.load(setOf(Conclusion(88, wineComment)))
        }.message shouldBe "Cannot load conclusions if there are are some stored already."
    }

    @Test
    fun load() {
        val teaConclusion = Conclusion(1, teaComment)
        val coffeeConclusion = Conclusion(2, coffeeComment)
        val wineConclusion = Conclusion(3, wineComment)
        val toLoad = setOf(teaConclusion, coffeeConclusion, wineConclusion)
        store.load(toLoad)
        store.all() shouldBe toLoad

        // Rebuild and check.
        store = PostgresConclusionStore(dbName)
        store.all() shouldBe toLoad
    }

    private fun getById(id: Int) = store.all().first { it.id == id }
}