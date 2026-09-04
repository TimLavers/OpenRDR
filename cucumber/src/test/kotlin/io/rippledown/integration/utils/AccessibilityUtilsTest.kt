package io.rippledown.integration.utils

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import javax.accessibility.AccessibleAction
import javax.accessibility.AccessibleContext
import kotlin.test.Test

class AccessibilityUtilsTest {

    @Test
    fun `performAccessibleClick invokes the first accessibility action`() {
        // given
        val context = mockk<AccessibleContext>()
        val action = mockk<AccessibleAction>()
        every { context.accessibleAction } returns action
        every { action.accessibleActionCount } returns 1
        every { action.doAccessibleAction(0) } returns true

        // when
        val clicked = context.performAccessibleClick()

        // then
        clicked shouldBe true
        verify(exactly = 1) { action.doAccessibleAction(0) }
    }

    @Test
    fun `performAccessibleClick returns false when the context has no action`() {
        // given
        val context = mockk<AccessibleContext>()
        every { context.accessibleAction } returns null

        // when
        val clicked = context.performAccessibleClick()

        // then
        clicked shouldBe false
    }

    @Test
    fun `performAccessibleClick returns false when the accessibility action list is empty`() {
        // given
        val context = mockk<AccessibleContext>()
        val action = mockk<AccessibleAction>()
        every { context.accessibleAction } returns action
        every { action.accessibleActionCount } returns 0

        // when
        val clicked = context.performAccessibleClick()

        // then
        clicked shouldBe false
        verify(exactly = 0) { action.doAccessibleAction(any()) }
    }
}
