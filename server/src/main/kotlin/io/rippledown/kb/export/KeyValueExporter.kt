package io.rippledown.kb.export

import io.rippledown.persistence.KeyValue
import io.rippledown.persistence.KeyValueStore
import java.util.regex.Pattern

class KeyValueExporter: Exporter<KeyValue>, Importer<KeyValue> {
    override fun exportToString(t: KeyValue) = "${t.id}\n${t.key}\n${t.value}"

    override fun importFromString(data: String): KeyValue {
        val parts = data.split(Pattern.compile("\n"), 3)
        return KeyValue(parts[0].toInt(), parts[1], parts[2])
    }
}
class KeyValueSource(val store: KeyValueStore, val type: String): IdentifiedObjectSource<KeyValue> {
    override fun all() = store.all()

    override fun idFor(t: KeyValue) = t.id

    override fun exporter() = KeyValueExporter()

    override fun exportType() = type

    override fun exportFileSuffix() = ".txt"
}