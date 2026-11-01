package io.rippledown.kb.chat

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.rippledown.fromJsonString
import io.rippledown.kb.chat.action.KbManagementAction
import io.rippledown.kb.chat.action.ListRecentChanges
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ActionCommentAwarenessTest {
    @Test
    fun `recent change options are bound from JSON by name`() {
        // Given
        val json = """{"action":"ListRecentChanges","limit":3,"attribute":"BMI"}"""

        // When
        val action = json.fromJsonString<ActionComment>().createActionInstance().shouldBeInstanceOf<ListRecentChanges>()

        // Then
        action.limit shouldBe 3
        action.attribute shouldBe "BMI"
    }

    @ParameterizedTest
    @ValueSource(strings = ["""{"action":"ListRecentChanges"}""", """{"action":"ListRecentChanges","limit":null,"attribute":null}"""])
    fun `absent or null options use the action defaults`(json: String) {
        // Given the JSON action

        // When
        val action = json.fromJsonString<ActionComment>().createActionInstance().shouldBeInstanceOf<ListRecentChanges>()

        // Then
        action.limit shouldBe 10
        action.attribute shouldBe null
    }

    @ParameterizedTest
    @ValueSource(strings = ["ListRecentChanges", "ListUsersPresent"])
    fun `awareness actions are available without a rule service and do not change context`(name: String) {
        // Given
        val json = """{"action":"$name"}"""

        // When
        val action = json.fromJsonString<ActionComment>().createActionInstance()

        // Then
        action.shouldBeInstanceOf<KbManagementAction>().changesContext shouldBe false
    }
}
