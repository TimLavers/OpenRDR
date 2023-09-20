package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
            conclusionManager.getById(conclusion.id)!!.text shouldBe text
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
}