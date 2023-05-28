package io.rippledown.persistence.postgres

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.rippledown.model.Attribute
import io.rippledown.model.beSameAs
import io.rippledown.model.condition.*
import io.rippledown.persistence.ConditionStore
import io.rippledown.util.shouldBeEqualUsingSameAs
import io.rippledown.util.shouldContainSameAs
import kotlin.test.BeforeTest
import kotlin.test.Test

class PostgresConditionStoreTest: PostgresStoreTest() {

    private val glucose = Attribute("Glucose", 1000)
    private val tsh = Attribute("TSH", 1001)
    private val notes = Attribute("Notes", 1002)
    private lateinit var store: ConditionStore

    override fun tableName() = CONDITIONS_TABLE

    @BeforeTest
    fun setup() {
        dropTable()
        store = postgresKB.conditionStore()
    }

    override fun reload() {
        super.reload()
        store = postgresKB.conditionStore()
    }

    @Test
    fun all() {
        store.all() shouldBe emptySet()
    }

    @Test
    fun create() {
        val inputGlucoseHigh = IsHigh(null, glucose)
        val createdGlucoseHigh = createAndCheck(inputGlucoseHigh)
        store.all().size shouldBe 1

        val inputTSHBorderlineLow = SlightlyLow(null, tsh, 12)
        val createdTSHBorderlineLow = createAndCheck(inputTSHBorderlineLow)
        store.all().size shouldBe 2

        val inputNotesSaysDiabetic = ContainsText(null, notes, "diabetic")
        val createdNotesSaysDiabetic = createAndCheck(inputNotesSaysDiabetic)
        store.all().size shouldBe 3

        // Rebuild and check.
        reload()
        store.all() shouldContainSameAs inputGlucoseHigh
        store.all() shouldContainSameAs inputTSHBorderlineLow
        store.all() shouldContainSameAs inputNotesSaysDiabetic
        store.all().size shouldBe 3
        store.all() shouldContain createdGlucoseHigh
        store.all() shouldContain createdTSHBorderlineLow
        store.all() shouldContain createdNotesSaysDiabetic
    }

    @Test
    fun load() {
        val toLoad = mutableSetOf<Condition>()
        repeat(100) {
            val condition = ContainsText(it, notes, "Stuff: $it")
            toLoad.add(condition)
        }
        store.load(toLoad)
        store.all() shouldBeEqualUsingSameAs toLoad

        // Rebuild and check.
        reload()
        store.all() shouldBeEqualUsingSameAs toLoad
    }

    @Test
    fun `cannot load if not empty`() {
        store.create(IsLow(null, tsh))
        shouldThrow<IllegalArgumentException> {
            store.load(emptySet())
        }
    }

    private fun createAndCheck(inputCondition: Condition): Condition {
        val createdCondition = store.create(inputCondition)
        createdCondition.id shouldNotBe null
        createdCondition should beSameAs(inputCondition)
        store.all() shouldContainSameAs inputCondition
        return createdCondition
    }
}