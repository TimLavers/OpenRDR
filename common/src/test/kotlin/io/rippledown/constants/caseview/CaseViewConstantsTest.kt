package io.rippledown.constants.caseview

import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CaseViewConstantsTest {

    @Test
    fun `user list section ids are built from the list name`() {
        // Given a user-defined list name
        // When the section ids are built
        // Then each id includes the name, so every list gets its own ids
        userListSectionId("Good") shouldBe "user_list_section_Good"
        userListSectionHeaderId("Good") shouldBe "user_list_section_header_Good"
    }

    @Test
    fun `user list section ids preserve the spelling of the list name`() {
        // Given list names in various spellings
        // When the section ids are built
        // Then the spelling is preserved, matching the displayed list name
        userListSectionId("good") shouldBe "user_list_section_good"
        userListSectionHeaderId("GOOD") shouldBe "user_list_section_header_GOOD"
        userListSectionId("Gestational Diabetes") shouldBe "user_list_section_Gestational Diabetes"
    }
}
