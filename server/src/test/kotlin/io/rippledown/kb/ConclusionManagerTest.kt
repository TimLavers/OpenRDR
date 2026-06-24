package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.CommentVariable
import io.rippledown.model.Conclusion
import io.rippledown.persistence.ConclusionStore
import io.rippledown.persistence.inmemory.InMemoryConclusionStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class ConclusionManagerTest {
    private val text1 = "Time for coffee!"
    private val text2 = "Time for tea!"
    private val text3 = "Time for champagne!"
    private lateinit var conclusionStore: ConclusionStore
    private lateinit var conclusionManager: ConclusionManager

    @BeforeTest
    fun setup() {
        conclusionStore = InMemoryConclusionStore()
        conclusionManager = ConclusionManager(conclusionStore)
    }

    @Test
    fun empty() {
        conclusionManager.all() shouldBe emptySet()
    }

    @Test
    fun getOrCreate() {
        val conclusion1 = conclusionManager.getOrCreate(text1)
        val conclusion2 = conclusionManager.getOrCreate(text2)
        val conclusion3 = conclusionManager.getOrCreate(text3)

        conclusion1.text shouldBe text1
        conclusion2.text shouldBe text2
        conclusion3.text shouldBe text3

        conclusionManager.all() shouldBe setOf(conclusion1, conclusion2, conclusion3)

        conclusionManager = ConclusionManager(conclusionStore)
        conclusionManager.all() shouldBe setOf(conclusion1, conclusion2, conclusion3)
    }

    @Test
    fun `create with existing text`() {
        val conclusion1 = conclusionManager.getOrCreate(text1)
        conclusionManager.getOrCreate(text2)
        conclusionManager.getOrCreate(text3)
        val conclusion3 = conclusionManager.getOrCreate(text1)
        conclusion3.text shouldBe text1
        conclusion3 shouldBe  conclusion1
    }

    @Test
    fun getById() {
        repeat(100) {
            val text = "Conclusion $it"
            val conclusion = conclusionManager.getOrCreate(text)
            conclusionManager.getById(conclusion.id) shouldBe conclusion
            conclusionManager.getById(conclusion.id).text shouldBe text
        }
    }

    @Test
    fun `should create a conclusion if the text is not found`() {
        val conclusion = conclusionManager.getOrCreate("Blah")
        conclusion.text shouldBe "Blah"
        conclusion.id shouldNotBe null
    }

    @Test
    fun `should not expect to get a conclusion with unknown id`() {
        shouldThrow<NoSuchElementException> { conclusionManager.getById(87654) }
    }

    @Test
    fun `load from persistent store`() {
        val conclusion1 = Conclusion(20, text1)
        val conclusion2 = Conclusion(21, text2)
        val conclusion3 = Conclusion(22, text3)
        conclusionStore.load(setOf(conclusion1, conclusion2, conclusion3))

        conclusionManager = ConclusionManager(conclusionStore)

        conclusionManager.all() shouldBe setOf(conclusion1, conclusion2, conclusion3)
        conclusionManager.getById(conclusion1.id) shouldBe conclusion1
        conclusionManager.getById(conclusion2.id) shouldBe conclusion2
        conclusionManager.getById(conclusion3.id) shouldBe conclusion3
    }

    // ==================== Comment Variable Tests ====================

    @Test
    fun `getOrCreate with variables`() {
        // Given
        val template = "Patient ${'$'}{} has glucose ${'$'}{} mmol/L"
        val variables1 = listOf(CommentVariable(1), CommentVariable(2))
        val variables2 = listOf(CommentVariable(3), CommentVariable(4))

        // When
        val conclusion1 = conclusionManager.getOrCreate(template, variables1)
        val conclusion2 = conclusionManager.getOrCreate(template, variables2)

        // Then
        conclusion1.text shouldBe template
        conclusion1.variables shouldBe variables1
        conclusion2.text shouldBe template
        conclusion2.variables shouldBe variables2
        conclusion1 shouldNotBe conclusion2 // Different variables = different conclusion
        conclusionManager.all().size shouldBe 2
    }

    @Test
    fun `getOrCreate with same text and variables returns same conclusion`() {
        // Given
        val template = "Patient ${'$'}{} has glucose ${'$'}{} mmol/L"
        val variables = listOf(CommentVariable(1), CommentVariable(2))

        // When
        val conclusion1 = conclusionManager.getOrCreate(template, variables)
        val conclusion2 = conclusionManager.getOrCreate(template, variables)

        // Then
        conclusion1 shouldBe conclusion2
        conclusionManager.all().size shouldBe 1
    }

    @Test
    fun `getOrCreate with plain text (no variables) uses empty variables list`() {
        // Given
        val plainText = "Normal results."

        // When
        val conclusion = conclusionManager.getOrCreate(plainText)

        // Then
        conclusion.text shouldBe plainText
        conclusion.variables shouldBe emptyList()
    }

    @Test
    fun `plain text and templated text with same text are different conclusions`() {
        // Given
        val template = "Patient ${'$'}{} has glucose ${'$'}{} mmol/L"
        val variables = listOf(CommentVariable(1), CommentVariable(2))

        // When
        val plainConclusion = conclusionManager.getOrCreate(template) // No variables
        val templatedConclusion = conclusionManager.getOrCreate(template, variables)

        // Then
        plainConclusion shouldNotBe templatedConclusion
        plainConclusion.variables shouldBe emptyList()
        templatedConclusion.variables shouldBe variables
        conclusionManager.all().size shouldBe 2
    }

    @Test
    fun `load from persistent store with variables`() {
        // Given
        val template1 = "Patient ${'$'}{} has glucose ${'$'}{} mmol/L"
        val variables1 = listOf(CommentVariable(1), CommentVariable(2))
        val template2 = "Glucose is ${'$'}{} mmol/L"
        val variables2 = listOf(CommentVariable(3))
        val plainText = "Normal results."

        val toLoad = setOf(
            Conclusion(20, template1, variables1),
            Conclusion(21, template2, variables2),
            Conclusion(22, plainText, emptyList())
        )
        conclusionStore.load(toLoad)

        // When
        conclusionManager = ConclusionManager(conclusionStore)

        // Then
        conclusionManager.all() shouldBe toLoad
        conclusionManager.getById(20) shouldBe toLoad.first { it.id == 20 }
        conclusionManager.getById(21) shouldBe toLoad.first { it.id == 21 }
        conclusionManager.getById(22) shouldBe toLoad.first { it.id == 22 }
    }
}