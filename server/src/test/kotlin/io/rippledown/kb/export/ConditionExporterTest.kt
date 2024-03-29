package io.rippledown.kb.export

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.rippledown.kb.AttributeManager
import io.rippledown.kb.ConditionManager
import io.rippledown.model.Attribute
import io.rippledown.model.condition.*
import io.rippledown.model.condition.greaterThanOrEqualTo
import io.rippledown.model.condition.isHigh
import io.rippledown.model.condition.isLow
import io.rippledown.persistence.inmemory.InMemoryAttributeStore
import io.rippledown.persistence.inmemory.InMemoryConditionStore
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class ConditionExporterTest {
    private val glucose = Attribute(100, "Glucose")
    private val ft4 = Attribute(101, "FT4")

    @Test
    fun exportToString() {
        val glucoseHigh = isHigh(99, glucose)
        ConditionExporter().exportToString(glucoseHigh) shouldContain glucose.id.toString()
    }

    @Test
    fun importFromString() {
        exportImport(isHigh(99, glucose))
        exportImport(greaterThanOrEqualTo(99, ft4, 3.0))
    }

    private fun exportImport(condition: Condition) {
        val exported = ConditionExporter().exportToString(condition)
        val imported = ConditionExporter().importFromString(exported)
        condition shouldBe imported
    }
}
class ConditionSourceTest {
    private lateinit var glucose : Attribute
    private lateinit var ft4: Attribute
    private lateinit var attributeManager: AttributeManager
    private lateinit var conditionManager: ConditionManager
    private lateinit var glucoseHigh: Condition
    private lateinit var ft4Low: Condition

    @BeforeEach
    fun init() {
        attributeManager = AttributeManager(InMemoryAttributeStore())
        glucose = attributeManager.getOrCreate("Glucose")
        ft4 = attributeManager.getOrCreate("FT4")
        conditionManager = ConditionManager(attributeManager, InMemoryConditionStore())
        glucoseHigh = conditionManager.getOrCreate(isHigh(null, glucose))
        ft4Low = conditionManager.getOrCreate(isLow(null, ft4))
    }

    @Test
    fun all() {
        ConditionSource(conditionManager).all() shouldBe setOf(glucoseHigh, ft4Low)
    }

    @Test
    fun exportType() {
        ConditionSource(conditionManager).exportType() shouldBe "Condition"
    }

    @Test
    fun exporter() {
        ConditionSource(conditionManager).exporter().shouldBeInstanceOf<ConditionExporter>()
    }

    @Test
    fun idFor() {
        ConditionSource(conditionManager).idFor(glucoseHigh) shouldBe glucoseHigh.id
    }
}