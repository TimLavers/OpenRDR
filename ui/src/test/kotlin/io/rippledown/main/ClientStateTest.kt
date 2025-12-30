package io.rippledown.main

import io.kotest.matchers.shouldBe
import io.rippledown.model.KBInfo
import kotlin.test.Test

class ClientStateTest {
    val newInfo = KBInfo("gl123", "Glucose")

    @Test
    fun currentKbTest() {
        val clientState = ClientState()
        clientState.currentKB() shouldBe null
        clientState.kbChanged(newInfo)
        clientState.currentKB() shouldBe newInfo
        clientState.kbChanged(null)
        clientState.currentKB() shouldBe null
    }

    @Test
    fun `listener is informed of new KB according to flag`() {
        var receivedKbInfo: KBInfo? = null
        fun callback(kbInfo: KBInfo?) { receivedKbInfo = kbInfo }

        val clientState = ClientState()
        clientState.attachListener { callback(it) }
        clientState.kbChanged(newInfo, notifyListeners = false)
        receivedKbInfo shouldBe null
        clientState.kbChanged(newInfo, notifyListeners = true)
        receivedKbInfo shouldBe newInfo

        clientState.kbChanged(null, notifyListeners = true)
        receivedKbInfo shouldBe null
    }

    @Test
    fun `newly attached listener displaces existing one`() {
        var receivedKbInfo: KBInfo? = null
        fun callback(kbInfo: KBInfo?) { receivedKbInfo = kbInfo }

        val clientState = ClientState()
        clientState.attachListener { callback(it) }

        var otherReceivedKbInfo: KBInfo? = null
        fun otherCallback(kbInfo: KBInfo?) { otherReceivedKbInfo = kbInfo }
        clientState.attachListener { otherCallback(it) }

        clientState.kbChanged(newInfo, notifyListeners = true)
        receivedKbInfo shouldBe null
        otherReceivedKbInfo shouldBe newInfo
    }
}
