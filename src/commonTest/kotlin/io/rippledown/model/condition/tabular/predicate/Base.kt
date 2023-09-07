package io.rippledown.model.condition.tabular.predicate

import io.kotest.matchers.shouldBe
import io.rippledown.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

open class Base {
    fun serializeDeserialize(testResultPredicate: TestResultPredicate): TestResultPredicate {
        val serialized = Json.encodeToString(testResultPredicate)
        return Json.decodeFromString(serialized)
    }
}