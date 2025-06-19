package io.rippledown.persistence.inmemory

import io.kotest.matchers.shouldBe
import io.rippledown.persistence.PersistentRule
import io.rippledown.persistence.RuleStore
import kotlin.test.BeforeTest
import kotlin.test.Test

class InMemoryRuleStoreTest {
    private lateinit var store: RuleStore
    private var index = 0

    @BeforeTest
    fun setup() {
        store = InMemoryRuleStore()
    }

    @Test
    fun `initially empty`() {
        store.all() shouldBe emptyList()
    }

    @Test
    fun `remove a rule`() {
        val prRoot = store.create(pr(null, null, ""))
        val pr2 = store.create(pr(prRoot.id, 10, "100,101"))
        val pr3 = store.create(pr(prRoot.id, 12, "100,103"))
        store.all() shouldBe listOf(prRoot, pr2, pr3)
        store.removeById(pr3.id!!)
        store.all() shouldBe listOf(prRoot, pr2)
        store.removeById(pr2.id!!)
        store.all() shouldBe listOf(prRoot)
    }

    private fun pr(parentId: Int?, conclusionId: Int?, conditionIds: String) = PersistentRule(null, parentId, conclusionId, conditionIds )
}