package io.rippledown.persistence.inmemory

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeKind
import io.rippledown.model.rule.AssignValue
import io.rippledown.model.rule.ByDefinition
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

    @Test
    fun `update a rule`() {
        // Given a stored rule that gives a conclusion
        val prRoot = store.create(pr(null, null, ""))
        val stored = store.create(pr(prRoot.id, 10, "100,101"))

        // When it is updated to assign a value instead
        val assignment = AssignValue(Attribute(7, "C1", AttributeKind.COMMENT), ByDefinition)
        val updated = stored.copy(conclusionId = null, assignment = assignment)
        store.update(updated)

        // Then the stored rule has the new form, same id
        store.all() shouldBe setOf(prRoot, updated)
    }

    @Test
    fun `updating a rule that is not in the store is not allowed`() {
        shouldThrow<IllegalArgumentException> {
            store.update(PersistentRule(99, null, null, ""))
        }
    }

    @Test
    fun `updating a rule with no id is not allowed`() {
        shouldThrow<IllegalArgumentException> {
            store.update(pr(null, 10, ""))
        }
    }

    private fun pr(parentId: Int?, conclusionId: Int?, conditionIds: String) = PersistentRule(null, parentId, conclusionId, conditionIds )
}