---
trigger: always_on
---

# Testing

- TDD: write the test before the implementation, and add a regression test for every bug fixed.
- Frameworks: `kotlin.test` `@Test`, kotest matchers (`shouldBe`, …), mockk.
- Never use `relaxed = true` on a mockk as this is set globally anyhow.
- Stub each call the test needs.
- Structure each test with `// Given`, `// When`, `// Then` comments.
- Never weaken or delete an assertion to get a green build. Deleting a test is acceptable only when the behaviour it
  pinned has genuinely gone — say so explicitly when you do it.
- Never reuse an attribute id across fixtures in the same test. `Attribute.equals` is id-only, so a duplicate id
  silently clobbers another attribute and can hang the rule-tree fixpoint loop.
- Test helpers already exist — use them rather than reinventing:
    - common `testFixtures`: `io.rippledown.utils.TestUtils` (`commentTexts`, `commentAssignment`,
      `createInterpretation`, `createViewableInterpretation`), `serializeDeserialize`, `defaultDate`, `daysAgo`.
    - server: `model/CommentFactory.kt`, `kb/CommentsOfCase.kt`, `kb/CommentAssignmentSessions.kt`,
      `server/CommentSessions.kt`, `model/rule/dsl/RuleDSL.kt`, `RuleTestBase`.
- Cucumber page objects: address a case through the section-scoped page object (`ProcessedCaseListPO`,
  `CornerstoneCaseListPO`, `FavouriteCaseListPO`). `CaseListPO` covers the whole panel only — a case name can appear in
  more than one section.
