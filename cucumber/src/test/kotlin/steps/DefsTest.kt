package steps

import io.mockk.*
import io.rippledown.integration.restclient.RESTClient
import org.junit.jupiter.api.Test

class DefsTest {
    @Test
    fun `the default KB step creates the default without selecting it again`() {
        // Given
        val client = mockk<RESTClient>()
        mockkStatic(::restClient)
        try {
            every { restClient() } returns client
            every { client.createKBWithDefaultName() } just Runs

            // When
            Defs().openDefaultKB()

            // Then
            verify(exactly = 1) { client.createKBWithDefaultName() }
            verify(exactly = 0) { client.selectKBByName(any()) }
        } finally {
            unmockkStatic(::restClient)
        }
    }
}
