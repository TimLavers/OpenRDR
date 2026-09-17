package io.rippledown.integration.pageobjects

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.rippledown.constants.interpretation.COMMENT_TEXT_PREFIX
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import javax.accessibility.AccessibleContext

class InterpretationPOTest {
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `hover waits for the comment row even if it disappears after its text is observed`(temporarilyMissing: Boolean) {
        // Given
        val comment = "Go to Bondi."
        val empty = mockk<AccessibleContext>()
        every { empty.accessibleDescription } returns null
        every { empty.accessibleChildrenCount } returns 0
        val cell = mockk<AccessibleContext>()
        every { cell.accessibleDescription } returns "${COMMENT_TEXT_PREFIX}C1"
        every { cell.accessibleChildrenCount } returns 0
        every { cell.accessibleText } returns null
        every { cell.accessibleName } returns comment
        val contextProvider = mockk<() -> AccessibleContext>()
        every { contextProvider() } returnsMany if (temporarilyMissing) listOf(empty, empty, cell) else listOf(cell)
        val page = spyk(InterpretationPO(contextProvider), recordPrivateCalls = true)
        every { page.commentsShown() } returns listOf(comment)
        every { page["movePointerOverCentreOf"](cell) } returns Unit

        // When
        page.movePointerToComment(comment)

        // Then
        verify(exactly = 1) { page["movePointerOverCentreOf"](cell) }
    }
}
