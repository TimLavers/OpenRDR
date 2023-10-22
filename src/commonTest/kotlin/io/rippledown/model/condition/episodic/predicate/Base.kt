package io.rippledown.model.condition.episodic.predicate

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

open class Base {
    fun serializeDeserialize(testResultPredicate: TestResultPredicate): TestResultPredicate {
        val serialized = Json.encodeToString(testResultPredicate)
        return Json.decodeFromString(serialized)
    }
}