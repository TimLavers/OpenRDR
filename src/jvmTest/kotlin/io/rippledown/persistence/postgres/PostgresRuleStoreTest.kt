package io.rippledown.persistence.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
        storedRoot.conclusionId shouldBe null
        storedRoot.conditionIds shouldBe emptySet()

        // Rebuild and check it's there.
        reload()
        store.all() shouldContain storedRoot
    }

    @Test
    fun create() {
        val pr1 = store.create(pr(0, 1, 1, 2, 3))
        store.all() shouldContain pr1
        pr1.id shouldNotBe null
        pr1.conclusionId shouldBe 1
        pr1.conditionIds shouldBe setOf(1, 2, 3)

        // Rebuild and check it's there.
        reload()

        store.all() shouldContain pr1
        store.all().size shouldBe 1
    }

    @Test
    fun `create with null conclusion id`() {
        val pr = PersistentRule(null, 7, null, setOf(23, 24))
        val created = store.create(pr)
        store.all() shouldContain created
        created.id shouldNotBe null
        created.conclusionId shouldBe null
        created.conditionIds shouldBe setOf(23, 24)

        // Rebuild and check it's there.
        reload()

        store.all() shouldContain created
        store.all().size shouldBe 1
    }

    @Test
    fun `create with no condition ids`() {
        val pr = PersistentRule(null, 7, 66, setOf())
        val created = store.create(pr)
        store.all() shouldContain created
        created.id shouldNotBe null
        created.parentId shouldBe 7
        created.conclusionId shouldBe 66
        created.conditionIds shouldBe setOf()

        // Rebuild and check it's there.
        reload()

        store.all() shouldContain created
        store.all().size shouldBe 1
    }

    @Test
    fun `create multiple`() {
        val pr1 = store.create(pr(12, 33, 8, 9, 10))
        val pr3 = store.create(pr(12, 33, 8, 9, 10))
        val pr2 = store.create(pr(pr1.id!!, 56, 8, 9, 5))
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
    fun all() {
        val rulesCreated = mutableSetOf<PersistentRule>()
        repeat(100) {
            rulesCreated.add(store.create(pr(it, 10, 100, 200)))
            store.all() shouldBe rulesCreated
        }

        reload()
        store.all() shouldBe rulesCreated
    }

    @Test
    fun `cannot load if not empty`() {
        store.create(pr(1, 2, 3))
        shouldThrow<IllegalArgumentException> {
            store.load(emptySet())
        }.message shouldBe "Cannot load persistent rules if there are some stored already."
    }

    @Test
    fun load() {
        val pr1 = PersistentRule( 1,0, 10, setOf(200, 201))
        val pr2 = PersistentRule(2, 1, 11, setOf(200, 201))
        val pr3 = PersistentRule(3, 2, 11, setOf(201, 202))
        val toLoad = setOf(pr1, pr2, pr3)
        store.load(toLoad)

        store.all() shouldBe toLoad
        reload()
        store.all() shouldBe toLoad
    }

    private fun pr(parentId: Int, conclusionId: Int, vararg conditionIds: Int)  = PersistentRule(null, parentId, conclusionId, conditionIds.toSet())
}