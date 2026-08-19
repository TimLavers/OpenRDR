package io.rippledown.persistence.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.rule.*
import io.rippledown.persistence.PersistentRule
import io.rippledown.persistence.RuleStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresRuleStoreTest: PostgresStoreTest() {
    private lateinit var store: RuleStore

    override fun tablesInDropOrder() = listOf(RULES_TABLE)

    @BeforeTest
    fun setup() {
        dropTable()
        store = postgresKB.ruleStore()
    }

    override fun reload() {
        super.reload()
        store = postgresKB.ruleStore()
    }

    @Test
    fun `initially empty`() {
        store.all() shouldBe emptySet()
    }

    @Test
    fun `store root rule`() {
        val storedRoot = store.create(PersistentRule())
        storedRoot.id shouldNotBe null
        storedRoot.parentId shouldBe null
        storedRoot.conditionIds shouldBe emptySet()

        // Rebuild and check it's there.
        reload()
        store.all() shouldContain storedRoot
    }

    @Test
    fun create() {
        val pr1 = store.create(pr(0, 1, 2, 3))
        store.all() shouldContain pr1
        pr1.id shouldNotBe null
        pr1.conditionIds shouldBe setOf(1, 2, 3)

        // Rebuild and check it's there.
        reload()

        store.all() shouldContain pr1
        store.all().size shouldBe 1
    }

    @Test
    fun `create with null parent id`() {
        val pr = PersistentRule(null, 7, setOf(23, 24))
        val created = store.create(pr)
        store.all() shouldContain created
        created.id shouldNotBe null
        created.conditionIds shouldBe setOf(23, 24)

        // Rebuild and check it's there.
        reload()

        store.all() shouldContain created
        store.all().size shouldBe 1
    }

    @Test
    fun `create with no condition ids`() {
        val pr = PersistentRule(null, 7, setOf())
        val created = store.create(pr)
        store.all() shouldContain created
        created.id shouldNotBe null
        created.parentId shouldBe 7
        created.conditionIds shouldBe setOf()

        // Rebuild and check it's there.
        reload()

        store.all() shouldContain created
        store.all().size shouldBe 1
    }

    @Test
    fun `create multiple`() {
        val pr1 = store.create(pr(12, 8, 9, 10))
        val pr3 = store.create(pr(12, 8, 9, 10))
        val pr2 = store.create(pr(pr1.id!!, 8, 9, 5))
        store.all() shouldContain pr1
        store.all() shouldContain pr2
        store.all() shouldContain pr3
        store.all().size shouldBe 3

        // Rebuild and check it's there.
        reload()

        store.all() shouldContain pr1
        store.all() shouldContain pr2
        store.all() shouldContain pr3
        store.all().size shouldBe 3
    }

   @Test
    fun remove() {
       val pr1 = store.create(pr(12, 8, 9, 10))
       val pr3 = store.create(pr(12, 8, 9, 10))
       val pr2 = store.create(pr(pr1.id!!, 8, 9, 5))
       with(store.all()) {
           this shouldContain pr1
           this shouldContain pr2
           this shouldContain pr3
           size shouldBe 3
       }
       store.removeById(pr3.id!!)
       with(store.all()) {
           this shouldContain pr1
           this shouldContain pr2
           size shouldBe 2
       }
       store.removeById(pr2.id!!)
       with(store.all()) {
           this shouldContain pr1
           size shouldBe 1
       }

        // Rebuild and check
        reload()

       with(store.all()) {
           this shouldContain pr1
           size shouldBe 1
       }
    }

    @Test
    fun all() {
        val rulesCreated = mutableSetOf<PersistentRule>()
        repeat(100) {
            rulesCreated.add(store.create(pr(it, 100, 200)))
            store.all() shouldBe rulesCreated
        }

        reload()
        store.all() shouldBe rulesCreated
    }

    @Test
    fun `cannot load if not empty`() {
        store.create(pr(1, 3))
        shouldThrow<IllegalArgumentException> {
            store.load(emptySet())
        }.message shouldBe "Cannot load persistent rules if there are some stored already."
    }

    @Test
    fun load() {
        val pr1 = PersistentRule(1, 0, setOf(200, 201))
        val pr2 = PersistentRule(2, 1, setOf(200, 201))
        val pr3 = PersistentRule(3, 2, setOf(201, 202))
        val toLoad = setOf(pr1, pr2, pr3)
        store.load(toLoad)

        store.all() shouldBe toLoad
        reload()
        store.all() shouldBe toLoad
    }

    @Test
    fun `create with an assignment`() {
        // Given a persistent rule with an assignment
        val diabetesStatus = Attribute(10, "Diabetes status", AttributeKind.DERIVED)
        val assignment = AssignValue(diabetesStatus, Literal("diabetic"))
        val pr = PersistentRule(null, 0, setOf(23), assignment)

        // When it is stored
        val created = store.create(pr)

        // Then the assignment is persisted and survives a reload
        created.assignment shouldBe assignment
        reload()
        store.all() shouldContain created
        store.all().single().assignment shouldBe assignment
    }

    @Test
    fun `load with assignments`() {
        // Given persistent rules with and without assignments
        val bmi = Attribute(11, "BMI", AttributeKind.DERIVED)
        val weight = Attribute(1, "weight")
        val formula = AssignValue(bmi, Formula(Binary(Operator.TIMES, AttributeValue(weight), Num(2.0))))
        val pr1 = PersistentRule(1, 0, setOf(200, 201))
        val pr2 = PersistentRule(2, 1, setOf(200), formula)
        val toLoad = setOf(pr1, pr2)

        // When they are loaded
        store.load(toLoad)

        // Then the assignments survive a reload
        store.all() shouldBe toLoad
        reload()
        store.all() shouldBe toLoad
    }

    @Test
    fun update() {
        // Given a stored rule
        val stored = store.create(pr(0, 100, 101))

        // When it is updated to assign a value instead
        val comment = Attribute(7, "C1", AttributeKind.COMMENT)
        val updated = stored.copy(assignment = AssignValue(comment, ByDefinition))
        store.update(updated)

        // Then the stored rule has the new form, same id, and survives a reload
        store.all() shouldBe setOf(updated)
        reload()
        store.all() shouldBe setOf(updated)
    }

    @Test
    fun `updating a rule that is not in the store is not allowed`() {
        shouldThrow<IllegalArgumentException> {
            store.update(PersistentRule(99, null, emptySet()))
        }.message shouldBe "Cannot update a rule that is not in the store."
    }

    @Test
    fun `updating a rule with no id is not allowed`() {
        shouldThrow<IllegalArgumentException> {
            store.update(PersistentRule(null, null, emptySet()))
        }.message shouldBe "Cannot update a rule that has no id."
    }

    private fun pr(parentId: Int, vararg conditionIds: Int) = PersistentRule(null, parentId, conditionIds.toSet())
}