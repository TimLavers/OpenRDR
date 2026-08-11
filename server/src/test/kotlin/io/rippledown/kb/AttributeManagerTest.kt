package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.persistence.AttributeStore
import io.rippledown.persistence.inmemory.InMemoryAttributeStore
import io.rippledown.util.randomString
import kotlin.test.BeforeTest
import kotlin.test.Test

class AttributeManagerTest {
    private lateinit var attributeManager: AttributeManager
    private lateinit var attributeStore: AttributeStore

    @BeforeTest
    fun setup() {
        attributeStore = InMemoryAttributeStore()
        attributeManager = AttributeManager(attributeStore)
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
        attributeStore.all() shouldBe setOf(a1)

        val a2 = attributeManager.getOrCreate("a2")
        a2.name shouldBe "a2"
        attributeManager.all() shouldBe setOf(a1, a2)
        attributeStore.all() shouldBe setOf(a1, a2)

        val a3 = attributeManager.getOrCreate("a2")//Existing name
        a3.name shouldBe "a2"
        a3 shouldBe a2
        attributeManager.all() shouldBe setOf(a1, a2)
        attributeStore.all() shouldBe setOf(a1, a2)
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
            initial.add(Attribute(it, randomString(12)))
        }
        attributeStore = InMemoryAttributeStore(initial)
        attributeManager = AttributeManager(attributeStore)
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

    @Test
    fun `getOrCreate without a kind creates an external attribute`() {
        // When an attribute is created without specifying a kind
        val glucose = attributeManager.getOrCreate("Glucose")

        // Then it is external
        glucose.kind shouldBe AttributeKind.EXTERNAL
    }

    @Test
    fun `getOrCreate with a kind creates an attribute of that kind`() {
        // When attributes are created with each KB-assigned kind
        val bmi = attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)
        val comment = attributeManager.getOrCreate("DiabetesStatus", AttributeKind.COMMENT)

        // Then the kinds are as requested, in the manager and in the store
        bmi.kind shouldBe AttributeKind.DERIVED
        comment.kind shouldBe AttributeKind.COMMENT
        attributeStore.all() shouldBe setOf(bmi, comment)
    }

    @Test
    fun `getOrCreate with a kind passes the kind to the store`() {
        // Given a manager backed by a mock store
        val mockStore = mockk<AttributeStore>()
        val bmi = Attribute(1, "BMI", AttributeKind.DERIVED)
        every { mockStore.all() } returns emptySet()
        every { mockStore.create("BMI", AttributeKind.DERIVED) } returns bmi
        val manager = AttributeManager(mockStore)

        // When a derived attribute is created
        val created = manager.getOrCreate("BMI", AttributeKind.DERIVED)

        // Then the store created it with the right kind
        created shouldBe bmi
        verify(exactly = 1) { mockStore.create("BMI", AttributeKind.DERIVED) }
    }

    @Test
    fun `getOrCreate with a kind returns the existing attribute of that kind`() {
        // Given a derived attribute
        val bmi = attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)

        // When it is requested again
        val again = attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)

        // Then the existing attribute is returned
        again shouldBe bmi
        attributeManager.all() shouldBe setOf(bmi)
    }

    @Test
    fun `getOrCreate with a kind rejects a name in use with a different kind`() {
        // Given an external attribute
        attributeManager.getOrCreate("Glucose")

        // When an attribute with the same name but a different kind is requested
        // Then the request is rejected
        shouldThrow<IllegalArgumentException> {
            attributeManager.getOrCreate("Glucose", AttributeKind.DERIVED)
        }.message shouldBe "An attribute with name Glucose already exists with kind EXTERNAL, not DERIVED."
    }

    @Test
    fun `getOrCreate with a kind rejects a name that differs only in case from an existing derived attribute`() {
        // Given a derived attribute
        attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)

        // When the same name with different case is requested for a derived attribute
        // Then the request is rejected
        shouldThrow<IllegalStateException> {
            attributeManager.getOrCreate("bmi", AttributeKind.DERIVED)
        }.message shouldBe "An attribute with name \"BMI\" already exists. Choose a different name."
    }

    @Test
    fun `getOrCreate with a kind rejects a derived attribute name that matches an external attribute ignoring case`() {
        // Given an external attribute
        attributeManager.getOrCreate("Glucose")

        // When a derived attribute with the same name ignoring case is requested
        // Then the request is rejected
        shouldThrow<IllegalStateException> {
            attributeManager.getOrCreate("glucose", AttributeKind.DERIVED)
        }.message shouldBe "An attribute with name \"Glucose\" already exists. Choose a different name."
    }

    @Test
    fun `getOrCreate without a kind returns an existing attribute regardless of its kind`() {
        // Given a derived attribute
        val bmi = attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)

        // When it is requested by name only, as condition parsing does
        val found = attributeManager.getOrCreate("BMI")

        // Then the existing derived attribute is returned
        found shouldBe bmi
        found.kind shouldBe AttributeKind.DERIVED
    }

    @Test
    fun `a comment attribute is created with the auto-generated name C1`() {
        // When a comment attribute is created without a name
        val comment = attributeManager.createCommentAttribute()

        // Then it is a COMMENT attribute named C1, in the manager and the store
        comment.name shouldBe "C1"
        comment.kind shouldBe AttributeKind.COMMENT
        attributeManager.byName("C1") shouldBe comment
        attributeStore.all() shouldBe setOf(comment)
    }

    @Test
    fun `successive comment attributes are auto-named with increasing indexes`() {
        // When several comment attributes are created
        val c1 = attributeManager.createCommentAttribute()
        val c2 = attributeManager.createCommentAttribute()
        val c3 = attributeManager.createCommentAttribute()

        // Then they are named C1, C2, C3
        c1.name shouldBe "C1"
        c2.name shouldBe "C2"
        c3.name shouldBe "C3"
    }

    @Test
    fun `auto-naming uses the smallest unused index`() {
        // Given a comment attribute explicitly named C2
        attributeManager.getOrCreate("C2", AttributeKind.COMMENT)

        // When comment attributes are auto-named
        val first = attributeManager.createCommentAttribute()
        val second = attributeManager.createCommentAttribute()

        // Then the gaps are filled first
        first.name shouldBe "C1"
        second.name shouldBe "C3"
    }

    @Test
    fun `auto-naming skips names used by attributes of any kind`() {
        // Given an external attribute named C1 and a derived attribute named C2
        attributeManager.getOrCreate("C1")
        attributeManager.getOrCreate("C2", AttributeKind.DERIVED)

        // When a comment attribute is auto-named
        val comment = attributeManager.createCommentAttribute()

        // Then the used names are skipped
        comment.name shouldBe "C3"
    }

    @Test
    fun `auto-naming skips names used by existing attributes ignoring case`() {
        // Given an external attribute named c1
        attributeManager.getOrCreate("c1")

        // When a comment attribute is auto-named
        val comment = attributeManager.createCommentAttribute()

        // Then the case-insensitive clash is avoided
        comment.name shouldBe "C2"
    }

    @Test
    fun `commentAttributes returns only the COMMENT attributes`() {
        // Given attributes of each kind
        attributeManager.getOrCreate("Glucose")
        attributeManager.getOrCreate("BMI", AttributeKind.DERIVED)
        val c1 = attributeManager.createCommentAttribute()
        val c2 = attributeManager.createCommentAttribute()

        // When the comment attributes are requested
        // Then only the COMMENT attributes are returned
        attributeManager.commentAttributes() shouldBe setOf(c1, c2)
    }

    @Test
    fun `commentAttributes is empty when there are none`() {
        attributeManager.getOrCreate("Glucose")
        attributeManager.commentAttributes() shouldBe emptySet()
    }

    @Test
    fun byName() {
        // Given an attribute
        val glucose = attributeManager.getOrCreate("Glucose")

        // When it is looked up by name
        // Then it is found, and unknown names are not
        attributeManager.byName("Glucose") shouldBe glucose
        attributeManager.byName("glucose").shouldBeNull()
        attributeManager.byName("Whatever").shouldBeNull()
    }
}