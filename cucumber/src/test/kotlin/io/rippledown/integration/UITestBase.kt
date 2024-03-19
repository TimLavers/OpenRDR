package io.rippledown.integration

import io.rippledown.integration.proxy.DirProxy
import io.rippledown.integration.proxy.LabProxy
import io.rippledown.integration.proxy.ServerProxy

open class UITestBase {
    val serverProxy = ServerProxy()
    val restClient = serverProxy.restClient()
    val attributeFactory = RestClientAttributeFactory(restClient)
    val conclusionFactory = RestClientConclusionFactory(restClient)
    val conditionFactory = RestClientConditionFactory(restClient)
    val labProxy = LabProxy(serverProxy.tempDir(), restClient)
    private val dirProxy = DirProxy()

    fun downloadsDir() = dirProxy.downloadsDir()

    fun resetKB() {
        restClient.createKBWithDefaultName()
    }
}