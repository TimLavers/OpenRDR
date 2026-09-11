package io.rippledown.integration.pageobjects

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.rippledown.chat.ChatTestHook
import io.rippledown.chat.KB_CHOICE_ITEM
import io.rippledown.chat.NUMBER_OF_CHAT_MESSAGES_
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import javax.accessibility.Accessible
import javax.accessibility.AccessibleAction
import javax.accessibility.AccessibleContext

class ChatPOTest {
    @AfterEach
    fun tearDown() = ChatTestHook.reset()

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `click the requested chip and retry an unsuccessful action`(firstClickSucceeds: Boolean) {
        // Given
        val root = mockk<AccessibleContext>()
        val chip = mockk<AccessibleContext>()
        val child = mockk<Accessible>()
        val action = mockk<AccessibleAction>()
        every { root.accessibleDescription } returns "${NUMBER_OF_CHAT_MESSAGES_}2"
        every { root.accessibleParent } returns mockk()
        every { root.accessibleChildrenCount } returns 1
        every { root.getAccessibleChild(0) } returns child
        every { child.accessibleContext } returns chip
        every { chip.accessibleDescription } returns "${KB_CHOICE_ITEM}Zoo Animals"
        every { chip.accessibleAction } returns action
        every { action.doAccessibleAction(0) } returnsMany listOf(firstClickSucceeds, true)
        ChatTestHook.update(emptyList(), sendIsEnabled = true)
        val page = ChatPO { root }

        // When
        page.clickKbChoice("Zoo Animals")

        // Then
        verify(exactly = if (firstClickSucceeds) 1 else 2) { action.doAccessibleAction(0) }
    }
}
