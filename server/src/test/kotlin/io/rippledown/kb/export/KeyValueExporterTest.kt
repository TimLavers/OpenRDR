package io.rippledown.kb.export

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.rippledown.kb.DESCRIPTION_KEY
import io.rippledown.persistence.KeyValue
import io.rippledown.persistence.KeyValueStore
import io.rippledown.persistence.inmemory.InMemoryKeyValueStore
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class KeyValueExporterTest : ExporterTestBase() {
    @Test
    fun exportToString() {
        val keyValue = KeyValue(60, "Issue", "More coffee needed.")
        with(KeyValueExporter().exportToString(keyValue)) {
            this shouldContain keyValue.id.toString()
            this shouldContain keyValue.key
            this shouldContain keyValue.value
        }
    }

    @Test
    fun importFromString() {
        val description = """
            # Description
            This is a multi-line description. 
            It includes a [link](https://en.wikipedia.org/wiki/Markdown). 
        """.trimIndent()

        val keyValue = KeyValue(999, DESCRIPTION_KEY, description)

        val exported = KeyValueExporter().exportToString(keyValue)
        with(KeyValueExporter().importFromString(exported)) {
            key shouldBe DESCRIPTION_KEY
            value shouldBe description
            id shouldBe keyValue.id
        }
    }
}
class KeyValueSourceTest {
    private lateinit var store: KeyValueStore
    private lateinit var source: KeyValueSource

    @BeforeEach
    fun init() {
        store = InMemoryKeyValueStore()
        source = KeyValueSource(store, "Things")
    }

    @Test
    fun all() {
        source.all() shouldBe emptySet()
        val drink = store.create("drink", "Tea")
        val eat = store.create("eat", "Noodles")
        with(source.all()) {
            size shouldBe 2
            this shouldContain drink
            this shouldContain eat
        }
    }

    @Test
    fun suffixTest() {
        source.exportFileSuffix() shouldBe ".txt"
    }

    @Test
    fun typeTest() {
        source.type shouldBe "Things"
    }

    @Test
    fun exporter() {
        val drink = store.create("drink", "Tea")
        val exported = source.exporter().exportToString(drink)
        val imported = KeyValueExporter().importFromString(exported)
        imported shouldBe drink
    }

    @Test
    fun idFor() {
        val drink = store.create("drink", "Tea")
        source.idFor(drink) shouldBe drink.id
    }
}