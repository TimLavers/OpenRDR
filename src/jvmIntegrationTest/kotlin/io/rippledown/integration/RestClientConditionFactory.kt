package io.rippledown.integration

import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.ConditionFactory
import io.rippledown.model.condition.Condition

class RestClientConditionFactory(private val restClient: RESTClient): ConditionFactory {
    override fun create(condition: Condition): Condition {
        TODO()
    }
}