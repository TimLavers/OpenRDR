package io.rippledown.kb.export

import io.rippledown.model.Attribute
import kotlinx.serialization.json.Json
import java.io.File

class AttributesImporter(private val source: File) {

    fun import(): Map<Int, Attribute> {
        val result = mutableMapOf<Int, Attribute>()
        source.readLines().filter { it.isNotBlank() }.forEach {
            val attribute = parse(it)
            result[attribute.id] = attribute
        }
        return result
    }

    private fun parse(line: String): Attribute {
        // Current format: one JSON object per line.
        return if (line.startsWith("{")) {
            Json.decodeFromString(line)
        } else {
            // Legacy format: "id name". The name is everything after the
            // first space; attributes exported this way are external.
            val parts = line.split(' ', limit = 2)
            Attribute(parts[0].toInt(), parts[1])
        }
    }
}