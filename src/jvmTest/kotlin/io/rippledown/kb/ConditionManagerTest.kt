package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.model.Attribute
import io.rippledown.model.Conclusion
import io.rippledown.model.beSameAs
import io.rippledown.model.condition.Condition
import io.rippledown.model.condition.ContainsText
import io.rippledown.model.condition.IsHigh
import io.rippledown.model.condition.IsNormal
import io.rippledown.persistence.*
import io.rippledown.util.shouldContainSameAs
import kotlin.test.BeforeTest
import kotlin.test.Test

class ConditionManagerTest {
    private lateinit var attributeManager: AttributeManager
    private lateinit var conditionStore: ConditionStore
    private lateinit var conditionManager: ConditionManager
    private lateinit var glucose: Attribute
    private lateinit var tsh: Attribute
    private lateinit var notes: Attribute

    @BeforeTest
    fun setup() {
        attributeManager = AttributeManager(InMemoryAttributeStore())
        conditionStore = InMemoryConditionStore()
        conditionManager = ConditionManager(attributeManager, conditionStore)
        glucose = attributeManager.getOrCreate("Glucose")
        tsh = attributeManager.getOrCreate("TSH")
        notes = attributeManager.getOrCreate("Notes")
    }

    @Test
    fun empty() {
        conditionManager.all() shouldBe emptySet()
    }

    @Test
    fun getOrCreate() {
        val glucoseHigh = IsNormal(null, glucose)
        val created = conditionManager.getOrCreate(glucoseHigh)
        created shouldNotBeSameInstanceAs glucoseHigh
        created.id shouldNotBe null
        created should beSameAs(glucoseHigh)

        // Rebuild.
        conditionManager = ConditionManager(attributeManager, conditionStore)
        val retrieved = conditionManager.getById(created.id!!)
        retrieved shouldNotBeSameInstanceAs glucoseHigh
        retrieved!!.id shouldNotBe null
        retrieved should beSameAs(glucoseHigh)
    }

    @Test
    fun `get or create when an equivalent condition is in manager`() {
        conditionManager.getOrCreate(IsHigh(null, tsh))
        conditionManager.getOrCreate(ContainsText(null, notes, "Cat"))
        val glucoseHigh = IsNormal(null, glucose)
        val created = conditionManager.getOrCreate(glucoseHigh)
        val createdAgain = conditionManager.getOrCreate(glucoseHigh)
        createdAgain shouldBeSameInstanceAs created

        // Rebuild.
        conditionManager = ConditionManager(attributeManager, conditionStore)
        val retrieved = conditionManager.getById(created.id!!)
        val createdYetAgain = conditionManager.getOrCreate(glucoseHigh)
        createdYetAgain shouldBeSameInstanceAs retrieved
    }

    @Test
    fun `cannot get or create for a condition that has non-null id`() {
        shouldThrow<IllegalArgumentException> {
            conditionManager.getOrCreate(IsHigh(88, glucose))
        }.message shouldBe "Cannot store a condition that has a non-null id."
    }

    @Test
    fun getById() {
        val glucoseNormal = conditionManager.getOrCreate(IsNormal(null, glucose))
        val tshNormal = conditionManager.getOrCreate(IsNormal(null, tsh))
        val notesIndicateFeline = conditionManager.getOrCreate(ContainsText(null, notes, "cat"))

        conditionManager.getById(glucoseNormal.id!!)!! should beSameAs(IsNormal(null, glucose))
        conditionManager.getById(tshNormal.id!!)!! should beSameAs(IsNormal(null, tsh))
        conditionManager.getById(notesIndicateFeline.id!!)!! should beSameAs(ContainsText(99, notes, "cat"))

        // Rebuild.
        conditionManager = ConditionManager(attributeManager, conditionStore)
        conditionManager.getById(glucoseNormal.id!!)!! should beSameAs(IsNormal(null, glucose))
        conditionManager.getById(tshNormal.id!!)!! should beSameAs(IsNormal(null, tsh))
        conditionManager.getById(notesIndicateFeline.id!!)!! should beSameAs(ContainsText(99, notes, "cat"))
    }

    @Test
    fun `get by id with unknown id`() {
        conditionManager.getOrCreate(IsNormal(null, glucose))
        conditionManager.getOrCreate(IsNormal(null, tsh))
        conditionManager.getById(9999) shouldBe null
    }

    @Test
    fun all() {
        repeat(100) {
            conditionManager.all().size shouldBe it
            val created = conditionManager.getOrCreate(ContainsText(null, notes, "Text_$it"))
            conditionManager.all() shouldContainSameAs created
        }

        // Rebuild.
        conditionManager = ConditionManager(attributeManager, conditionStore)
        conditionManager.all().size shouldBe 100
    }

    @Test
    fun `load from persistent store`() {
        val toLoad = mutableSetOf<Condition>()
        repeat(100) {
            toLoad.add(ContainsText(it, notes, "Blah: $it"))
        }
        conditionStore.load(toLoad)

        // Rebuild.
        conditionManager = ConditionManager(attributeManager, conditionStore)
        conditionManager.all().size shouldBe 100
        toLoad.forEach{
            conditionManager.all() shouldContainSameAs it
        }
    }
}