package io.rippledown.server

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import io.rippledown.constants.server.IN_MEMORY
import io.rippledown.constants.server.STARTING_SERVER
import org.slf4j.Logger
import kotlin.test.BeforeTest
import kotlin.test.Test

class MainTest {
    private lateinit var mockLogger: Logger

    @BeforeTest
    fun setUp() {
        mockLogger = mockk()
        mockkObject(OpenRDRServer)
        every { OpenRDRServer.logger } returns mockLogger
    }

    @Test
    fun `should be able to call main() and verify that the server has started`() {
        //When
        Thread {
            main(args = arrayOf(IN_MEMORY))
        }.apply {
            isDaemon = true
            start()
        }

        //Then
        verify(timeout = 2_000) { mockLogger.info(STARTING_SERVER) }
    }
}