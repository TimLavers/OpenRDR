package io.rippledown.persistence.postgres

import io.kotest.matchers.collections.haveSize
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.persistence.PersistentRule
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresRuleStoreTest: PostgresStoreTest() {
    private lateinit var store: PostgresRuleStore

    override fun tableName() = RULES_TABLE

    @BeforeTest
    fun setup() {
        dropTable()
        store = PostgresRuleStore(dbName)
    }

    @Test
    fun `initially empty`() {
        store.all() shouldBe emptySet()
    }

    @Test
    fun create() {
        val pr1 = store.create(pr(0, 1, 1, 2, 3))
        store.all() shouldContain pr1
        pr1.id shouldNotBe null
        pr1.conclusionId shouldBe 1
        pr1.conditionIds shouldBe setOf(1, 2, 3)

        // Rebuild and check it's there.
        store = PostgresRuleStore(dbName)

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
        store = PostgresRuleStore(dbName)

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
        store = PostgresRuleStore(dbName)

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
        store = PostgresRuleStore(dbName)

        store.all() shouldContain pr1
        store.all() shouldContain pr2
        store.all() shouldContain pr3
        store.all().size shouldBe 3
    }

    private fun pr(parentId: Int, conclusionId: Int, vararg conditionIds: Int)  = PersistentRule(null, parentId, conclusionId, conditionIds.toSet())

}