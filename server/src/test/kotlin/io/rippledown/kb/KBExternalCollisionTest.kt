package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.rippledown.model.AttributeKind
import io.rippledown.model.KBInfo
import io.rippledown.model.Result
import io.rippledown.model.external.ExternalCase
import io.rippledown.model.external.MeasurementEvent
import io.rippledown.persistence.inmemory.InMemoryKB
import io.rippledown.utils.defaultDate
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Externally supplied attribute names that collide with KB-assigned
 * attributes are deterministically mangled at case ingestion, so that
 * case processing never fails and external data is never silently
 * dropped. See documentation/design/repeat_inferencing.md.
 */
class KBExternalCollisionTest {
    private lateinit var kb: KB

    @BeforeTest
    fun setup() {
        kb = KB(InMemoryKB(KBInfo("id123", "TestKB")))
    }

    private fun externalCase(name: String, vararg attributeToValue: Pair<String, String>) =
        ExternalCase(name, attributeToValue.associate { MeasurementEvent(it.first, defaultDate) to Result(it.second) })

    @Test
    fun `an external name with no existing attribute creates an external attribute`() {
        // When a case with a new attribute name is ingested
        val case = kb.createRDRCase(externalCase("Fermi", "Glucose" to "12.0"))

        // Then an external attribute with that name holds the value
        val glucose = kb.attributeManager.byName("Glucose")!!
        glucose.kind shouldBe AttributeKind.EXTERNAL
        case.latestValue(glucose) shouldBe "12.0"
    }

    @Test
    fun `an external name matching an external attribute reuses it`() {
        // Given an existing external attribute
        val glucose = kb.attributeManager.getOrCreate("Glucose")

        // When a case with that attribute name is ingested
        val case = kb.createRDRCase(externalCase("Fermi", "Glucose" to "12.0"))

        // Then the existing attribute holds the value
        case.latestValue(glucose) shouldBe "12.0"
        kb.attributeManager.all() shouldBe setOf(glucose)
    }

    @Test
    fun `an external name colliding with a derived attribute is mangled`() {
        // Given a derived attribute named "Risk score"
        val derived = kb.attributeManager.getOrCreate("Risk score", AttributeKind.DERIVED)

        // When a case arrives with an external attribute of the same name
        val case = kb.createRDRCase(externalCase("Fermi", "Risk score" to "7"))

        // Then the value is stored under the mangled external attribute
        val mangled = kb.attributeManager.byName("Risk score (external)")!!
        mangled.kind shouldBe AttributeKind.EXTERNAL
        case.latestValue(mangled) shouldBe "7"

        // And the derived attribute has no value in the case
        case.latestValue(derived) shouldBe null
    }

    @Test
    fun `an external name colliding with a comment attribute is mangled`() {
        // Given a comment attribute named "DiabetesStatus"
        kb.attributeManager.getOrCreate("DiabetesStatus", AttributeKind.COMMENT)

        // When a case arrives with an external attribute of the same name
        val case = kb.createRDRCase(externalCase("Fermi", "DiabetesStatus" to "diabetic"))

        // Then the value is stored under the mangled external attribute
        val mangled = kb.attributeManager.byName("DiabetesStatus (external)")!!
        mangled.kind shouldBe AttributeKind.EXTERNAL
        case.latestValue(mangled) shouldBe "diabetic"
    }

    @Test
    fun `the same external name maps to the same mangled attribute on every case`() {
        // Given a derived attribute and a first case with a colliding name
        kb.attributeManager.getOrCreate("Risk score", AttributeKind.DERIVED)
        kb.createRDRCase(externalCase("Fermi", "Risk score" to "7"))
        val mangled = kb.attributeManager.byName("Risk score (external)")!!

        // When another case arrives with the same colliding name
        val secondCase = kb.createRDRCase(externalCase("Curie", "Risk score" to "3"))

        // Then the values stay together under the one mangled attribute
        secondCase.latestValue(mangled) shouldBe "3"
        kb.attributeManager.all().filter { it.name.startsWith("Risk score") }.size shouldBe 2
    }

    @Test
    fun `mangling applies when a cornerstone case is added from an external case`() {
        // Given a derived attribute named "Risk score"
        kb.attributeManager.getOrCreate("Risk score", AttributeKind.DERIVED)

        // When an external case with a colliding name is added as a cornerstone
        val cornerstone = kb.addCornerstoneCase(externalCase("Fermi", "Risk score" to "7"))

        // Then the value is stored under the mangled external attribute
        val mangled = kb.attributeManager.byName("Risk score (external)")!!
        cornerstone.latestValue(mangled) shouldBe "7"
    }

    @Test
    fun `processing a case with a colliding name does not fail`() {
        // Given a derived attribute named "Risk score"
        kb.attributeManager.getOrCreate("Risk score", AttributeKind.DERIVED)

        // When a case with a colliding name is processed
        val processed = kb.processCase(externalCase("Fermi", "Risk score" to "7", "Glucose" to "12.0"))

        // Then the case is stored with both values
        val mangled = kb.attributeManager.byName("Risk score (external)")!!
        val glucose = kb.attributeManager.byName("Glucose")!!
        processed.latestValue(mangled) shouldBe "7"
        processed.latestValue(glucose) shouldBe "12.0"
    }

    @Test
    fun `externalAttributeFor is deterministic`() {
        // Given a derived attribute
        kb.attributeManager.getOrCreate("Alpha", AttributeKind.DERIVED)

        // When the external attribute for the colliding name is requested twice
        val first = kb.externalAttributeFor("Alpha")
        val second = kb.externalAttributeFor("Alpha")

        // Then the same mangled attribute is returned both times
        first shouldBeSameInstanceAs second
        first.name shouldBe "Alpha (external)"
    }

    @Test
    fun mangledExternalName() {
        mangledExternalName("Risk score") shouldBe "Risk score (external)"
    }
}
