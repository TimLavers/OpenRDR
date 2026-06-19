package io.rippledown.persistence.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.CommentVariable
import io.rippledown.model.Conclusion
import io.rippledown.persistence.ConclusionStore
import org.apache.commons.lang3.RandomStringUtils
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresConclusionStoreTest: PostgresStoreTest() {
    private val teaComment = "Time for a refreshing cup of tea!"
    private val coffeeComment = "Time for a revivifying cup of coffee!"
    private val wineComment = "Time for a strengthening glass of cab sav!"

    private lateinit var store: ConclusionStore

    override fun tablesInDropOrder() = listOf(CONCLUSION_VARIABLES_TABLE, CONCLUSIONS_TABLE)

    @BeforeTest
    fun setup() {
        dropTable()
        store = postgresKB.conclusionStore()
    }

    override fun reload() {
        super.reload()
        store = postgresKB.conclusionStore()
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
        reload()
        store.all() shouldBe conclusionsCreated
    }

    @Test
    fun `can create long conclusions`() {
        val longComment = RandomStringUtils.random(2048)
        val created = store.create(longComment)

        store.all() shouldBe setOf(created)

        // Rebuild and check.
        reload()
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
        reload()
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
        reload()
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
        reload()
        store.all() shouldBe toLoad
    }

    private fun getById(id: Int) = store.all().first { it.id == id }

    // ==================== Comment Variable Tests ====================

    @Test
    fun `create conclusion with variables`() {
        // Given
        val template = "Patient ${'$'}{} has glucose ${'$'}{} mmol/L"
        val variables = listOf(CommentVariable(8, 1), CommentVariable(24, 2))

        // When
        val created = store.create(template, variables)

        // Then
        created.text shouldBe template
        created.variables shouldBe variables
        store.all() shouldBe setOf(created)

        // Rebuild and check
        reload()
        val loaded = getById(created.id)
        loaded.text shouldBe template
        loaded.variables shouldBe variables
    }

    @Test
    fun `create plain conclusion (no variables) with empty variables list`() {
        // Given
        val plainText = "Normal results."

        // When
        val created = store.create(plainText, emptyList())

        // Then
        created.text shouldBe plainText
        created.variables shouldBe emptyList()
        store.all() shouldBe setOf(created)

        // Rebuild and check
        reload()
        val loaded = getById(created.id)
        loaded.text shouldBe plainText
        loaded.variables shouldBe emptyList()
    }

    @Test
    fun `cannot create conclusion with same text and variables as existing`() {
        // Given
        val template = "Patient ${'$'}{} has glucose ${'$'}{} mmol/L"
        val variables1 = listOf(CommentVariable(8, 1), CommentVariable(24, 2))
        val variables2 = listOf(CommentVariable(8, 1), CommentVariable(24, 2))
        store.create(template, variables1)

        // When/Then
        shouldThrow<IllegalArgumentException> {
            store.create(template, variables2)
        }.message shouldBe "A conclusion with the given text already exists."
    }

    @Test
    fun `can create conclusion with same text but different variables`() {
        // Given
        val template = "Patient ${'$'}{} has glucose ${'$'}{} mmol/L"
        val variables1 = listOf(CommentVariable(8, 1), CommentVariable(24, 2))
        val variables2 = listOf(CommentVariable(8, 3), CommentVariable(24, 4))
        store.create(template, variables1)

        // When
        val created2 = store.create(template, variables2)

        // Then
        created2.text shouldBe template
        created2.variables shouldBe variables2
        store.all().size shouldBe 2
    }

    @Test
    fun `store conclusion with variables`() {
        // Given
        val template = "Patient ${'$'}{} has glucose ${'$'}{} mmol/L"
        val variables1 = listOf(CommentVariable(8, 1), CommentVariable(24, 2))
        val variables2 = listOf(CommentVariable(8, 3), CommentVariable(24, 4))
        val created = store.create(template, variables1)

        // When
        store.store(Conclusion(created.id, template, variables2))

        // Then
        val updated = getById(created.id)
        updated.text shouldBe template
        updated.variables shouldBe variables2

        // Rebuild and check
        reload()
        val reloaded = getById(created.id)
        reloaded.text shouldBe template
        reloaded.variables shouldBe variables2
    }

    @Test
    fun `load conclusions with variables`() {
        // Given
        val template1 = "Patient ${'$'}{} has glucose ${'$'}{} mmol/L"
        val variables1 = listOf(CommentVariable(8, 1), CommentVariable(24, 2))
        val template2 = "Glucose is ${'$'}{} mmol/L"
        val variables2 = listOf(CommentVariable(11, 3))
        val plainText = "Normal results."

        val toLoad = setOf(
            Conclusion(1, template1, variables1),
            Conclusion(2, template2, variables2),
            Conclusion(3, plainText, emptyList())
        )

        // When
        store.load(toLoad)

        // Then
        store.all() shouldBe toLoad

        // Rebuild and check
        reload()
        store.all() shouldBe toLoad
    }
}