package io.rippledown.integration

import io.rippledown.integration.restclient.RESTClient
import io.rippledown.model.Conclusion
import io.rippledown.model.ConclusionFactory

class RestClientConclusionFactory(private val restClient: RESTClient) : ConclusionFactory {
    override fun getOrCreate(text: String): Conclusion {
        return restClient.getOrCreateConclusion(text)
    }

    override fun getById(id: Int): Conclusion {
        TODO("Not yet implemented")
    }
}