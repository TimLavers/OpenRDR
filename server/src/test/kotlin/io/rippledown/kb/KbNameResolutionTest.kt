package io.rippledown.kb

import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import io.rippledown.sample.SampleKB
import io.rippledown.sample.SampleKB.TSH
import io.rippledown.sample.SampleKB.ZOO
import kotlin.test.Test

class KbNameResolutionTest {
    private val thyroids = KBInfo("thyroids_1", "Thyroids")
    private val thyroidsOld = KBInfo("thyroids_2", "Thyroids (old)")
    private val glucose = KBInfo("glucose_1", "Glucose")
    private val lipids = KBInfo("lipids_1", "Lipids")
    private val all = listOf(thyroids, thyroidsOld, glucose, lipids)
    private val stored = listOf(thyroids, glucose)
    private val demos = SampleKB.demonstrations()
    private val demoTitles =
        listOf("Contact Lense Prescription", "Pathology", "Thyroid Stimulating Hormone", "Zoo Animals")


    @Test
    fun `demonstration exact title`() {
        // Given
        val kbInfos = stored

        // When
        val resolution = resolveKbName("Zoo Animals", kbInfos, demos)

        // Then
        resolution shouldBe KbResolution.Demonstration(ZOO)
    }

    @Test
    fun `demonstration exact title ignores case and whitespace`() {
        // Given
        val kbInfos = stored

        // When
        val resolution = resolveKbName("  zoo animals  ", kbInfos, demos)

        // Then
        resolution shouldBe KbResolution.Demonstration(ZOO)
    }

    @Test
    fun `demonstration unique partial title ignores case`() {
        // Given
        val kbInfos = stored

        // When
        val resolution = resolveKbName("zOo", kbInfos, demos)

        // Then
        resolution shouldBe KbResolution.Demonstration(ZOO)
    }

    @Test
    fun `stored partial match takes precedence over a demonstration partial match`() {
        // Given
        val kbInfos = stored

        // When
        val resolution = resolveKbName("Thyroid", kbInfos, demos)

        // Then
        resolution shouldBe KbResolution.Partial(thyroids)
    }

    @Test
    fun `exact demonstration title resolves with stored knowledge bases`() {
        // Given
        val kbInfos = stored

        // When
        val resolution = resolveKbName("Thyroid Stimulating Hormone", kbInfos, demos)

        // Then
        resolution shouldBe KbResolution.Demonstration(TSH)
    }

    @Test
    fun `exact demonstration takes precedence over a stored partial match`() {
        // Given
        val kbInfos = listOf(KBInfo("tsh_copy", "Thyroid Stimulating Hormone copy"))

        // When
        val resolution = resolveKbName("Thyroid Stimulating Hormone", kbInfos, demos)

        // Then
        resolution shouldBe KbResolution.Demonstration(TSH)
    }

    @Test
    fun `stored exact match takes precedence over demonstration exact match`() {
        // Given
        val pathology = KBInfo("pathology_1", "Pathology")
        val kbInfos = listOf(pathology)

        // When
        val resolution = resolveKbName("Pathology", kbInfos, demos)

        // Then
        resolution shouldBe KbResolution.Exact(pathology)
    }

    @Test
    fun `stored exact ambiguity takes precedence over a demonstration`() {
        // Given
        val kbInfos = listOf(KBInfo("p1", "PATHOLOGY"), KBInfo("p2", "pathology"))

        // When
        val resolution = resolveKbName("Pathology", kbInfos, demos)

        // Then
        resolution shouldBe KbResolution.Ambiguous("Pathology", listOf("PATHOLOGY", "pathology"))
    }

    @Test
    fun `ambiguous stored partial matches include matching demonstrations sorted`() {
        // Given
        val kbInfos = stored

        // When
        val resolution = resolveKbName("o", kbInfos, demos)

        // Then
        resolution shouldBe KbResolution.Ambiguous(
            "o",
            listOf(
                "Contact Lense Prescription",
                "Glucose",
                "Pathology",
                "Thyroid Stimulating Hormone",
                "Thyroids",
                "Zoo Animals"
            )
        )
    }

    @Test
    fun `several demonstration partial matches are ambiguous`() {
        // Given
        val kbInfos = listOf(lipids)

        // When
        val resolution = resolveKbName("o", kbInfos, demos)

        // Then
        resolution shouldBe KbResolution.Ambiguous("o", demoTitles)
    }

    @Test
    fun `unknown name lists stored and demonstration titles sorted`() {
        // Given
        val kbInfos = stored

        // When
        val resolution = resolveKbName(" Nothing ", kbInfos, demos)

        // Then
        resolution shouldBe KbResolution.NotFound("Nothing", listOf("Glucose", "Thyroids"), demoTitles)
    }

    @Test
    fun `blank name does not partially match demonstrations`() {
        // Given
        val kbInfos = stored

        // When
        val resolution = resolveKbName("   ", kbInfos, demos)

        // Then
        resolution shouldBe KbResolution.NotFound("", listOf("Glucose", "Thyroids"), demoTitles)
    }

    @Test
    fun `unknown name with only demonstrations lists their titles`() {
        // Given
        val kbInfos = emptyList<KBInfo>()

        // When
        val resolution = resolveKbName("Nothing", kbInfos, demos)

        // Then
        resolution shouldBe KbResolution.NotFound("Nothing", emptyList(), demoTitles)
    }

    @Test
    fun `demonstrations can be explicitly disabled`() {
        // Given
        val kbInfos = stored

        // When
        val resolution = resolveKbName("Zoo Animals", kbInfos, demonstrations = emptyList())

        // Then
        resolution shouldBe KbResolution.NotFound("Zoo Animals", listOf("Glucose", "Thyroids"))
    }

    @Test
    fun `demonstration title check requires a full title and ignores case and whitespace`() {
        // Given
        val names = listOf("zoo animals", "Zoo", " Pathology ", "", "   ", "Nothing", "Zoo Animals - cases only")

        // When
        val matches = names.map { isDemonstrationTitle(it, demos) }

        // Then
        matches shouldBe listOf(true, false, true, false, false, false, false)
    }

    @Test
    fun `demonstration title check with no demonstrations`() {
        // Given
        val demonstrations = emptyList<SampleKB>()

        // When
        val matches = isDemonstrationTitle("Pathology", demonstrations)

        // Then
        matches shouldBe false
    }

    @Test
    fun `exact match`() {
        // Given / When
        val resolution = resolveKbName("Glucose", all)

        // Then
        resolution shouldBe KbResolution.Exact(glucose)
    }

    @Test
    fun `exact match ignores case and surrounding whitespace`() {
        // Given / When
        val resolution = resolveKbName("  gLUCOSE ", all)

        // Then
        resolution shouldBe KbResolution.Exact(glucose)
    }

    @Test
    fun `an exact match is preferred to a partial one`() {
        // Given "Thyroids" is both an exact match and a substring of "Thyroids (old)"
        // When
        val resolution = resolveKbName("Thyroids", all)

        // Then
        resolution shouldBe KbResolution.Exact(thyroids)
    }

    @Test
    fun `several case-insensitive exact matches prefer the one with identical case`() {
        // Given
        val lower = KBInfo("kb_1", "thyroids")
        val upper = KBInfo("kb_2", "THYROIDS")
        val exact = KBInfo("kb_3", "Thyroids")

        // When
        val resolution = resolveKbName("Thyroids", listOf(lower, upper, exact))

        // Then
        resolution shouldBe KbResolution.Exact(exact)
    }

    @Test
    fun `several case-insensitive exact matches with none identical is ambiguous`() {
        // Given
        val lower = KBInfo("kb_1", "thyroids")
        val upper = KBInfo("kb_2", "THYROIDS")

        // When
        val resolution = resolveKbName("Thyroids", listOf(lower, upper))

        // Then
        resolution shouldBe KbResolution.Ambiguous("Thyroids", listOf("THYROIDS", "thyroids"))
    }

    @Test
    fun `unique partial match`() {
        // Given / When
        val resolution = resolveKbName("gluc", all)

        // Then
        resolution shouldBe KbResolution.Partial(glucose)
    }

    @Test
    fun `partial match ignores case`() {
        // Given / When
        val resolution = resolveKbName("LIPID", all)

        // Then
        resolution shouldBe KbResolution.Partial(lipids)
    }

    @Test
    fun `several partial matches are ambiguous, candidates sorted`() {
        // Given / When
        val resolution = resolveKbName("thyroid", all)

        // Then
        resolution shouldBe KbResolution.Ambiguous("thyroid", listOf("Thyroids", "Thyroids (old)"))
    }

    @Test
    fun `no match lists what is available, sorted`() {
        // Given / When
        val resolution = resolveKbName("Irons", all)

        // Then
        resolution shouldBe KbResolution.NotFound("Irons", listOf("Glucose", "Lipids", "Thyroids", "Thyroids (old)"))
    }

    @Test
    fun `blank name is not found and does not match everything`() {
        // Given / When
        val resolution = resolveKbName("   ", all)

        // Then
        resolution shouldBe KbResolution.NotFound("", listOf("Glucose", "Lipids", "Thyroids", "Thyroids (old)"))
    }

    @Test
    fun `no knowledge bases at all`() {
        // Given / When
        val resolution = resolveKbName("Glucose", emptyList())

        // Then
        resolution shouldBe KbResolution.NotFound("Glucose", emptyList())
    }

    @Test
    fun `near duplicate when the new name is contained in an existing name`() {
        // Given / When / Then
        nearDuplicateOf("Thyroid", all) shouldBe thyroids
    }

    @Test
    fun `near duplicate when an existing name is contained in the new name`() {
        // Given / When / Then
        nearDuplicateOf("Glucose 2", all) shouldBe glucose
    }

    @Test
    fun `near duplicate ignores case`() {
        // Given / When / Then
        nearDuplicateOf("lipid", all) shouldBe lipids
    }

    @Test
    fun `an identical name is not a near duplicate, it is a clash`() {
        // Given / When / Then
        nearDuplicateOf("Glucose", all) shouldBe null
        nearDuplicateOf("glucose", all) shouldBe null
    }

    @Test
    fun `unrelated name has no near duplicate`() {
        // Given / When / Then
        nearDuplicateOf("Irons", all) shouldBe null
    }

    @Test
    fun `blank new name has no near duplicate`() {
        // Given / When / Then
        nearDuplicateOf("  ", all) shouldBe null
    }

    @Test
    fun `when several existing names are near duplicates the shortest is reported`() {
        // Given "Thyroid" is contained in both "Thyroids" and "Thyroids (old)"
        // When / Then
        nearDuplicateOf("Thyroid", all) shouldBe thyroids
    }
}
