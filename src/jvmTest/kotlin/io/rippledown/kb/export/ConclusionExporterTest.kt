package io.rippledown.kb.export

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.rippledown.kb.ConclusionManager
import io.rippledown.model.Conclusion
import io.rippledown.persistence.InMemoryConclusionStore
import org.junit.Before
import kotlin.test.Test

class ConclusionExporterTest {

    @Test
    fun exportToString() {
        val conclusion = Conclusion(65, "More coffee needed.")

        ConclusionExporter().exportToString(conclusion) shouldContain conclusion.id.toString()
        ConclusionExporter().exportToString(conclusion) shouldContain conclusion.text
    }

    @Test
    fun importFromString() {
        val conclusion = Conclusion(65, "More coffee needed.")

        val exported = ConclusionExporter().exportToString(conclusion)
        val imported = ConclusionExporter().importFromString(exported)
        conclusion shouldBe imported
    }
}
class ConclusionSourceTest {
    private lateinit var conclusionManager: ConclusionManager
    private lateinit var teaConclusion: Conclusion
    private lateinit var  coffeeConclusion: Conclusion
    private lateinit var  cocoaConclusion: Conclusion

    @Before
    fun init() {
        conclusionManager = ConclusionManager(InMemoryConclusionStore())
        teaConclusion = conclusionManager.getOrCreate("Time for tea.")
        coffeeConclusion = conclusionManager.getOrCreate("Time for coffee.")
        cocoaConclusion = conclusionManager.getOrCreate("Time for cocoa.")
    }

    @Test
    fun all() {
        ConclusionSource(conclusionManager).all() shouldBe setOf(teaConclusion, cocoaConclusion, coffeeConclusion)
    }

    @Test
    fun exportType() {
        ConclusionSource(conclusionManager).exportType() shouldBe "Conclusion"
    }

    @Test
    fun exporter() {
        ConclusionSource(conclusionManager).exporter().shouldBeInstanceOf<ConclusionExporter>()
    }

    @Test
    fun idFor() {
        ConclusionSource(conclusionManager).idFor(teaConclusion) shouldBe teaConclusion.id
    }
}