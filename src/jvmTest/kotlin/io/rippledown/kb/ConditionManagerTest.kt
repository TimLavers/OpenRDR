package io.rippledown.kb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.rippledown.model.*
import io.rippledown.model.condition.*
import io.rippledown.model.condition.TabularCondition
import io.rippledown.persistence.ConditionStore
import io.rippledown.persistence.inmemory.InMemoryAttributeStore
import io.rippledown.persistence.inmemory.InMemoryConditionStore
import io.rippledown.util.shouldContainSameAs
import java.time.Instant
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
        val glucoseHigh = isNormal(null, glucose)
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
        conditionManager.getOrCreate(isHigh(null, tsh))
        conditionManager.getOrCreate(containsText(null, notes, "Cat"))
        val glucoseNormal = isNormal(null, glucose)
        val created = conditionManager.getOrCreate(glucoseNormal)
        val createdAgain = conditionManager.getOrCreate(glucoseNormal)
        createdAgain shouldBeSameInstanceAs created

        // Rebuild.
        conditionManager = ConditionManager(attributeManager, conditionStore)
        val retrieved = conditionManager.getById(created.id!!)
        val createdYetAgain = conditionManager.getOrCreate(glucoseNormal)
        createdYetAgain shouldBeSameInstanceAs retrieved
    }

    @Test
    fun `cannot get or create for a condition that has non-null id`() {
        shouldThrow<IllegalArgumentException> {
            conditionManager.getOrCreate(isHigh(88, glucose))
        }.message shouldBe "Cannot store a condition that has a non-null id."
    }

    @Test
    fun getById() {
        val glucoseNormal = conditionManager.getOrCreate(isNormal(null, glucose))
        val tshNormal = conditionManager.getOrCreate(isNormal(null, tsh))
        val notesIndicateFeline = conditionManager.getOrCreate(containsText(null, notes, "cat"))

        conditionManager.getById(glucoseNormal.id!!)!! should beSameAs(isNormal(null, glucose))
        conditionManager.getById(tshNormal.id!!)!! should beSameAs(isNormal(null, tsh))
        conditionManager.getById(notesIndicateFeline.id!!)!! should beSameAs(containsText(99, notes, "cat"))

        // Rebuild.
        conditionManager = ConditionManager(attributeManager, conditionStore)
        conditionManager.getById(glucoseNormal.id!!)!! should beSameAs(isNormal(null, glucose))
        conditionManager.getById(tshNormal.id!!)!! should beSameAs(isNormal(null, tsh))
        conditionManager.getById(notesIndicateFeline.id!!)!! should beSameAs(containsText(99, notes, "cat"))
    }

    @Test
    fun `get by id with unknown id`() {
        conditionManager.getOrCreate(isNormal(null, glucose))
        conditionManager.getOrCreate(isNormal(null, tsh))
        conditionManager.getById(9999) shouldBe null
    }

    @Test //Cond-2
    fun `created conditions use attributes from attribute manager`() {
        val glucoseCopy = glucose.copy()
        val templateCondition = isHigh(null, glucoseCopy)
        val createdCondition = conditionManager.getOrCreate(templateCondition) as TabularCondition
        createdCondition.attribute shouldBeSameInstanceAs glucose

        // Rebuild.
        conditionManager = ConditionManager(attributeManager, conditionStore)
        val retrieved = conditionManager.getById(createdCondition.id!!) as TabularCondition
        retrieved.attribute shouldBeSameInstanceAs glucose
    }

    @Test //Cond-2
    fun `loaded conditions should use attributes from attribute manager`() {
        val glucoseCopy = glucose.copy()
        val condition = isHigh(900, glucoseCopy)
        conditionStore.load(setOf(condition))

        conditionManager = ConditionManager(attributeManager, conditionStore)
        val loadedCondition = conditionManager.all().single() as TabularCondition
        loadedCondition.attribute shouldBeSameInstanceAs glucose
    }

    @Test
    fun all() {
        repeat(100) {
            conditionManager.all().size shouldBe it
            val created = conditionManager.getOrCreate(containsText(null, notes, "Text_$it"))
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
            toLoad.add(containsText(it, notes, "Blah: $it"))
        }
        conditionStore.load(toLoad)

        // Rebuild.
        conditionManager = ConditionManager(attributeManager, conditionStore)
        conditionManager.all().size shouldBe 100
        toLoad.forEach {
            conditionManager.all() shouldContainSameAs it
        }
    }

    @Test
    fun `should return NotBlank for every attribute that is in the current episode`() {
        val a1 = attributeManager.getOrCreate("A1")
        val a2 = attributeManager.getOrCreate("A2")

        val caseAttributes = listOf(a1, a2)
        val viewableCase = createCase(caseAttributes)
        val conditionHints = conditionManager.conditionHintsForCase(viewableCase)
        conditionHints.conditions shouldHaveSize 2
        conditionHints.conditions[0].asText() shouldBe "A1 is not blank"
        conditionHints.conditions[1].asText() shouldBe "A2 is not blank"
    }

    @Test
    fun `every condition returned in the condition hints should have an id`() {
        val a1 = attributeManager.getOrCreate("A1")
        val caseAttributes = listOf(a1)
        val viewableCase = createCase(caseAttributes)
        val conditionHints = conditionManager.conditionHintsForCase(viewableCase)
        conditionHints.conditions.forEach { condition -> condition.id shouldNotBe null }
    }

    private fun createCase(attributes: List<Attribute>): RDRCase {
        val date = Instant.now()
        val builder = RDRCaseBuilder()
        attributes.forEach {
            builder.addResult(it, date.toEpochMilli(), TestResult(it.name + " value"))
        }
        return builder.build("")
    }
}