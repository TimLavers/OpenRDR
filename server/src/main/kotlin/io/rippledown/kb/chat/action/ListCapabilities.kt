package io.rippledown.kb.chat.action

import io.rippledown.model.chat.CapabilitySection
import io.rippledown.model.chat.ChatResponse

class ListCapabilities : Action {
    fun response(hasCase: Boolean): ChatResponse {
        val knowledgeBases = CapabilitySection(
            "Knowledge bases", listOf(
                "list, open, create, close, delete or rename a knowledge base",
                "show or change a knowledge base description",
                "open your own named copy of a demonstration knowledge base",
                "import a knowledge base from a ZIP archive (no knowledge base needs to be open)",
                "export the open knowledge base to a ZIP archive",
                "add a demonstration case to try rule building in an empty knowledge base"
            )
        )
        val sections = buildList {
            add(knowledgeBases)
            if (hasCase) {
                add(
                    CapabilitySection(
                        "Report comments", listOf(
                            "add, remove or replace a report comment (with a rule)",
                            "insert a case value into a comment using an attribute name in braces, e.g. {TSH}"
                        )
                    )
                )
                add(
                    CapabilitySection(
                        "Derived attributes", listOf(
                            "add a derived attribute and assign a value, e.g. BMI = weight / height ^ 2 (with a rule)",
                            "remove or replace a derived value (with a rule)",
                            "edit a derived attribute's definition everywhere it is used (without building a rule)"
                        )
                    )
                )
                add(
                    CapabilitySection(
                        "Building a rule", listOf(
                            "see suggested reasons for building a rule",
                            "add, list or remove reasons in the current rule",
                            "review cornerstone cases",
                            "cancel the rule you are currently building",
                            "undo the last rule"
                        )
                    )
                )
                add(
                    CapabilitySection(
                        "Names and layout", listOf(
                            "rename a comment or a derived attribute",
                            "rename a condition's phrase everywhere it is used",
                            "reorder the attributes"
                        )
                    )
                )
                add(
                    CapabilitySection(
                        "Favourite cases", listOf(
                            "copy the current case to favourites, optionally with a new name",
                            "delete the current case from favourites"
                        )
                    )
                )
            }
        }
        val text = sections.joinToString("\n\n") { section ->
            section.heading + ":\n" + section.items.joinToString("\n") { "- $it" }
        }
        return ChatResponse(text, capabilities = sections)
    }
}
