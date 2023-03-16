package io.rippledown.integration

import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Attribute
import io.rippledown.model.AttributeFactory

class RestClientAttributeFactory(val restClient: RESTClient): AttributeFactory {
    override fun create(name: String): Attribute {
        return restClient.getOrCreateAttribute(name)
    }
}