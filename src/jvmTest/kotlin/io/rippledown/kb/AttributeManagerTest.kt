package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.Attribute
import io.rippledown.util.randomString
import kotlin.test.BeforeTest
import kotlin.test.Test

class AttributeManagerTest {
    private lateinit var attributeManager: AttributeManager

    @BeforeTest
    fun setup() {
        attributeManager = AttributeManager()
    }

    @Test
    fun empty() {
        attributeManager.all() shouldBe emptySet()
    }

    @Test
    fun getOrCreate() {
        val a1 = attributeManager.getOrCreate("a1")
        a1.name shouldBe "a1"
        attributeManager.all() shouldBe setOf(a1)

        val a2 = attributeManager.getOrCreate("a2")
        a2.name shouldBe "a2"
        attributeManager.all() shouldBe setOf(a1, a2)

        val a3 = attributeManager.getOrCreate("a2")//Existing name
        a3.name shouldBe "a2"
        a3 shouldBe a2
        attributeManager.all() shouldBe setOf(a1, a2)
    }

    @Test //Attr-4
    fun `no duplicate names`() {
        val a1 = attributeManager.getOrCreate("aardvarks")
        a1.name shouldBe "aardvarks"

        val a2 = attributeManager.getOrCreate("aardvarks")
        a2 shouldBe a1
        attributeManager.all() shouldBe setOf(a1)
    }

    @Test //Attr-5
    fun `attribute names are case sensitive`() {
        val a1 = attributeManager.getOrCreate("aardvarks")
        a1.name shouldBe "aardvarks"

        val a2 = attributeManager.getOrCreate("Aardvarks")
        a2 shouldNotBe a1
        a2.name shouldBe "Aardvarks"
        attributeManager.all() shouldBe setOf(a1, a2)
    }

    @Test
    fun `construct with set of attributes`() {
        val initial = mutableSetOf<Attribute>()
        repeat(100) {
            initial.add(Attribute(randomString(12), it))
        }
        attributeManager = AttributeManager(initial)
        initial.forEach{
            it shouldBe attributeManager.getById(it.id)
            it.name shouldBe attributeManager.getById(it.id).name
        }
    }

    @Test
    fun getById() {
        val stored = mutableListOf<Attribute>()
        repeat(100) {
            stored.add(attributeManager.getOrCreate(randomString(12)))
        }
        stored.forEach{
            it shouldBe attributeManager.getById(it.id)
            it.name shouldBe attributeManager.getById(it.id).name
        }
    }

    @Test
    fun `get by id when attribute not in manager`() {
        attributeManager.getOrCreate("Whatever")
        attributeManager.getOrCreate("Stuff")
        shouldThrow<NoSuchElementException> {
            attributeManager.getById(99)
        }
    }

    @Test
    fun `get by id when empty`() {
        shouldThrow<NoSuchElementException> {
            attributeManager.getById(99)
        }
    }
}