# Step 16 — converting the remaining tests from conclusions to comment attributes

This is a **mechanical conversion task**. All production code is already converted and compiles; `:common:test` is
green. What remains is the
`:server` test sources, then `:ui` and `:cucumber`.

Do **not** redesign anything. Do **not** delete a test to make it compile. Every test that exists pins behaviour that
must still hold; convert it.

Background, if needed: `documentation/design/repeat_inferencing.md`,
"Phase 2 — comments become derived attributes" and resolved decision 9.

---

## 1. The change, in one paragraph

A comment used to be a `Conclusion` (an id and a text) referenced by a rule. Now a comment is an **attribute** of kind
`COMMENT` whose *definition* is the comment's text, and a rule **assigns** that attribute. So:

| Before                                     | After                                                                                                                       |
|--------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| `Conclusion(1, "Glucose is high.")`        | an `Attribute(id, "C1", AttributeKind.COMMENT)` whose definition is `CommentTemplate("Glucose is high.")`                   |
| `Rule(id, parent, conclusion, conditions)` | `Rule(id, parent, conditions, mutableSetOf(), assignment)`                                                                  |
| `rule.conclusion`                          | `rule.assignment` (an `AssignValue`)                                                                                        |
| `interpretation.conclusions()`             | `interpretation.assignments()`                                                                                              |
| `interpretation.conclusionTexts()`         | `kb.commentsFor(case)` (test helper, see §2)                                                                                |
| `ChangeTreeToAddConclusion(c)`             | `ChangeTreeToAddAssignment(AssignValue(attr, ByDefinition))` — but in most tests, call the session entry point instead (§3) |
| `kb.conclusionManager.getOrCreate(t)`      | nothing: the comment attribute is created by the session entry point                                                        |

**Deleted types** — if you see any of these, they are gone for good:
`Conclusion`, `ConclusionFactory`, `ConclusionProvider`, `ConclusionManager`,
`InMemoryConclusionStore`, `PostgresConclusionStore`, `AssignConclusion`,
`ChangeTreeToAddConclusion` / `…Remove…` / `…Replace…`,
`AddConclusionRuleTreeChanger` / `ModifyConclusionRuleTreeChanger` /
`RemoveConclusionRuleTreeChanger` / `ReplaceConclusionRuleTreeChanger`,
`DummyConclusionFactory`, `Rule.conclusion`, `RuleSummary.conclusion`,
`PersistentRule.conclusionId`, `Interpretation.conclusions()`,
`Interpretation.conclusionTexts()`, `Interpretation.conditionsForConclusion()`,
`Interpretation.idsOfRulesGivingConclusion()`,
`KBEndpoint.startRuleSessionToAddConclusion` (and Remove/Replace),
`KBEndpoint.getOrCreateConclusion`, `KBEndpoint.allConclusions`,
`RuleTreeChange.alignWith`, `PersistentKB.conclusionStore()`.

**Signatures that changed** — memorise these three:

```kotlin
// Rule: the conclusion parameter is gone; assignment is last.
Rule(id: Int, parent: Rule? = null, conditions: Set<Condition> = setOf(),
childRules: MutableSet<Rule> = mutableSetOf(), assignment: AssignValue? = null)

// RuleSummary: same.
RuleSummary(id: Int = 0, conditions: Set<Condition> = setOf(),
conditionTextsFromRoot: List<String> = listOf(), assignment: AssignValue? = null)

// PersistentRule: conclusionId is gone.
PersistentRule(id: Int?, parentId: Int?, conditionIds: Set<Int>, assignment: AssignValue? = null)
PersistentRule(id: Int?, parentId: Int?, conditionIdsString: String, assignment: AssignValue? = null)

// RuleFactory: one method, and a null assignment means a stopping rule.
fun createRuleAndAddToParent(parent: Rule, assignment: AssignValue?, conditions: Set<Condition>): Rule

// RuleManager: no conclusion manager.
RuleManager(conditionManager, attributeProvider, ruleStore)

// InterpretationViewManager: no arguments at all.
InterpretationViewManager()
```

---

## 2. The helpers you must use (they already exist — do not write new ones)

**`RuleTestBase`** (`common/src/testFixtures/.../model/rule/RuleTestBase.kt`), which most rule tests already extend:

```kotlin
fun commentAttribute(text: String): Attribute   // one COMMENT attribute per text, memoised
fun comment(text: String): AssignValue          // AssignValue(commentAttribute(text), CommentTemplate(text))
fun createCondition(text: String): Condition
fun checkInterpretation(interpretation: Interpretation, vararg assignments: AssignValue)
```

**`CommentFactory`** (`server/src/test/.../model/CommentFactory.kt`) — the replacement for `DummyConclusionFactory`, for
tests that are not
`RuleTestBase` subclasses and for the rule DSL:

```kotlin
val commentFactory = CommentFactory()
commentFactory.attributeFor("Go to Bondi.")   // Attribute, COMMENT kind
commentFactory.comment("Go to Bondi.")        // AssignValue
```

**`kb.commentsFor(case)`** (`server/src/test/.../kb/CommentsOfCase.kt`) — the set of comment texts a KB gives a case.
Use it for every assertion that used to read `case.interpretation.conclusionTexts()`. It interprets the case and
resolves by-definition assignments, which is necessary because the rule holds only `ByDefinition`; the text lives in the
definition store.

**Endpoint helpers** (`server/src/test/.../server/CommentSessions.kt`):

```kotlin
endpoint.startRuleSessionToAddComment(caseId, "text")
endpoint.startRuleSessionToRemoveComment(caseId, "text")
endpoint.startRuleSessionToReplaceComment(caseId, "old", "new")
endpoint.commentsForCase(caseId)
```

**The rule DSL** (`server/src/test/.../model/rule/dsl/RuleDSL.kt`) now takes a
`CommentFactory` and its keyword is `comment`, not `conclusion`:

```kotlin
tree = ruleTree(commentFactory) {
    child {
        id = 1
        comment { "ConcA" }
        condition(conditionFactory) { attribute = clinicalNotes; constant = "a" }
    }
}.build()
```

---

## 3. The four conversion recipes

### Recipe A — a rule session in a test that has a real `KB`

Use the comment entry points on `RuleSessionManager`. They create the comment attribute, store its definition, and build
the `ByDefinition` assignment.

```kotlin
// BEFORE
rsm.startRuleSession(case, ChangeTreeToAddConclusion(kb.conclusionManager.getOrCreate("Go to Bondi.")))
// AFTER
rsm.startRuleSessionToAddComment(case, "Go to Bondi.")

// BEFORE
rsm.startRuleSession(case, ChangeTreeToRemoveConclusion(conclusion))
// AFTER  (the case must be interpreted first, so the comment is present to remove)
kb.interpret(case)
rsm.startRuleSessionToRemoveComment(case, "Go to Bondi.")

// BEFORE
rsm.startRuleSession(case, ChangeTreeToReplaceConclusion(old, new))
// AFTER
kb.interpret(case)
rsm.startRuleSessionToReplaceComment(case, "old text", "new text")
```

`startRuleSessionToRemoveComment` and `…ReplaceComment` **fail** if no comment attribute with that text exists, so the
add-rule must have been committed first. If a test previously removed a conclusion that was never added, that test needs
an add-and-commit first; keep the assertion it was making.

### Recipe B — a rule built directly, with no KB

```kotlin
// BEFORE
private val conclusion1 = Conclusion(100, "First conclusion")
val rule = Rule(1, null, conclusion1, setOf(createCondition("a")))
// AFTER  (inside a RuleTestBase subclass)
private val comment1 = comment("First comment")
val rule = Rule(1, null, setOf(createCondition("a")), mutableSetOf(), comment1)
```

Note the **argument order change**: conditions move to third position. A call like `Rule(0, null, null, setOf())` (a
stopping rule) becomes
`Rule(0, null, setOf())`.

### Recipe C — a tree change object, where the test is really about the change

```kotlin
// BEFORE
val action = ChangeTreeToAddConclusion(Conclusion(1, "A"))
// AFTER
val action = ChangeTreeToAddAssignment(comment("A"))

// BEFORE
val action = ChangeTreeToRemoveConclusion(conclusion)
// AFTER
val action = ChangeTreeToRemoveAssignment(comment("A"))

// BEFORE
val action = ChangeTreeToReplaceConclusion(old, new)
// AFTER
val action = ChangeTreeToReplaceAssignment(comment("A"), comment("B"))
```

`ChangeTreeToReplaceAssignment` requires both attributes to be COMMENT kind (or the same attribute), which
`comment(...)` satisfies.

`toString()` assertions change accordingly, e.g.
`"ChangeTreeToAddAssignment(toBeAdded=$assignment)"`. Build the expected string from the assignment value rather than
hard-coding it.

### Recipe D — assertions

```kotlin
case.interpretation.conclusions()            -> case.interpretation.assignments()
case.interpretation.conclusionTexts()        -> kb.commentsFor(case)
interpretation.conclusions().map { it.text } -> kb.commentsFor(case)
rule.conclusion                              -> rule.assignment
rule.conclusion?.text shouldBe "X"           -> rule.assignment shouldBe comment("X")
interpretation.conditionsForConclusion(c)    -> interpretation.conditionsForAssignment(assignment)
interpretation.idsOfRulesGivingConclusion(c) -> interpretation.idsOfRulesMakingAssignment(assignment)
```

Where a test asserted on a conclusion's *text* and the rule now holds
`ByDefinition`, assert through the definition store:

```kotlin
kb.derivedDefinitionManager.definitionFor(rule.assignment!!.attribute.id) shouldBe CommentTemplate("X")
```

---

## 4. Order of work, file by file

Work down this list. After each file, run:

```powershell
.\gradlew.bat :server:compileTestKotlin --console=plain 2>&1 | Select-String "^e: " | Select-Object -First 20
```

and to see what is left overall:

```powershell
.\gradlew.bat :server:compileTestKotlin --console=plain 2>&1 | Select-String "^e: " |
  ForEach-Object { ($_.Line -replace 'file:///C:/repos/OpenRDR/server/src/test/kotlin/io/rippledown/','') } |
  Group-Object { ($_ -split ':')[1] } | Sort-Object Count -Descending | Select-Object Count, Name
```

### 4.1 First, one shared file that blocks others

**`model/rule/RuleTreeTest.kt`** contains `DummyRuleFactory`, used by several other test files. Fix it first:

```kotlin
class DummyRuleFactory : RuleFactory {
    override fun createRuleAndAddToParent(parent: Rule, assignment: AssignValue?, conditions: Set<Condition>) =
        Rule(0, parent, conditions, mutableSetOf(), assignment)
}
```

The rest of that file: the DSL calls have already been renamed to
`commentFactory` / `comment { }`; convert the remaining
`tree.ruleForId(1).conclusion?.text shouldBe "ConcA"` to
`tree.ruleForId(1).assignment shouldBe commentFactory.comment("ConcA")`, and
`checkInterpretationForCase`'s `it.text` to the comment texts via
`(it.expression as CommentTemplate).text`.

### 4.2 Then, in this order (highest error count first)

For each: apply recipes A–D. None of these needs a design decision.

1. `model/rule/FurtherRuleTests.kt` — recipe B throughout. It declares
   `conclusion1/2/3` at the top; replace with `comment1 = comment("First comment")` etc.
2. `kb/RuleSessionManagerTest.kt` — recipe A throughout (it has a real KB).
3. `model/rule/RuleTreeChangeTest.kt` — recipe C.
4. `model/rule/RuleTreeChangerTest.kt` — recipe C, plus the changer classes are now only `AddAssignmentRuleTreeChanger`,
   `ModifyAssignmentRuleTreeChanger`,
   `RemoveAssignmentRuleTreeChanger`, `ReplaceAssignmentRuleTreeChanger`.
5. `persistence/PersistentRuleTest.kt` — the `conclusionId` argument is gone from every constructor call;
   `pr.conclusionId shouldBe 10` becomes an assertion on `pr.assignment`. Tests named `no conclusion` become
   `no assignment`.
6. `kb/RuleManagerTest.kt` — drop `conclusionManager` from the fixture and from the `RuleManager(...)` call;
   `coffeeConclusion` etc. become
   `coffeeComment = AssignValue(attributeManager.getOrCreate("C1", AttributeKind.COMMENT), CommentTemplate(text1))`.
7. `suggestions/scorer/HistoricalRuleScorerTest.kt` — the scorer now matches on the **assigned attribute id**, so
   `goToBondi` becomes an `AssignValue` and the rules assign it. Keep every scoring assertion exactly as it is.
8. `model/rule/RuleTreeFixpointTest.kt` — recipes B and C.
9. `kb/KBExemptCornerstoneTest.kt` — recipe A.
10. `model/rule/AssignmentRuleTreeChangeTest.kt` — only the `Rule(...)` argument order and any `alignWith` calls (delete
    the `alignWith` assertions; that method is gone because there is nothing to align).
11. `model/rule/RuleBuildingSessionForChangeToAddConclusionTest.kt` — rename the file and class to
    `…ForChangeToAddCommentTest`, recipe C. Same for
    `…ForChangeToRemoveConclusionTest` → `…ForChangeToRemoveCommentTest` and
    `RuleBuildingSessionForChangeToReplaceConclusion.kt` →
    `RuleBuildingSessionForChangeToReplaceComment.kt`. **Keep every scenario.**
12. `kb/InterpretationViewManagerTest.kt` — `InterpretationViewManager()` takes no arguments now, and the
    conclusion-rendering tests are gone from the production class, so those tests are replaced by equivalent comment
    assignment tests (several already exist in that file — keep them, delete only the ones that construct a
    `Conclusion`, since an identical assignment test is already present alongside each).
13. `kb/RuleManagerAssignmentTest.kt`, `persistence/PersistentRuleAssignmentTest.kt`,
    `persistence/postgres/PostgresRuleStoreTest.kt`,
    `persistence/postgres/MultipleDBsRuleStoresTest.kt`,
    `persistence/inmemory/InMemoryRuleStoreTest.kt` — constructor argument order only.
14. `suggestions/HistoricalConditionInjectionTest.kt`,
    `suggestions/RelevanceRankerTest.kt`,
    `suggestions/ConditionSuggesterCycleTest.kt`,
    `suggestions/scorer/CommentTokenOverlapScorerTest.kt` — recipe C.
15. `kb/export/RuleExporterTest.kt`, `kb/export/ExportedRuleTest.kt`,
    `kb/export/IdentifiedObjectExporterTest.kt`,
    `kb/export/DirectoryImporterTest.kt`, `kb/export/KBImporterTest.kt` —
    `PersistentRule` argument order; any `ConclusionExporter` /
    `ConclusionSource` usage is deleted (use `DefinitionExporter` /
    `DefinitionSource` if the test needs an exporter of that shape).
16. Everything else in the list is one to six errors: `kb/WithResolvedDefinitionsTest.kt`,
    `model/rule/DerivedAttributeDependencyGraphTest.kt`,
    `model/rule/RuleBuildingSessionDerivedValuesTest.kt`,
    `model/rule/RuleBuildingSessionTest.kt`,
    `model/rule/RuleSessionRecorderTest.kt`, `kb/KBSelectCornerstoneTest.kt`,
    `kb/KBSessionTest.kt`, `kb/report/ReportServiceTest.kt`,
    `kb/EditDerivedDefinitionTest.kt`, `kb/KBProcessCaseResolutionTest.kt`,
    `kb/RuleSessionManagerCommentAssignmentTest.kt`,
    `kb/chat/KBChatServiceTest.kt`, `server/OpenRDRServerTestBase.kt`
    (delete the `conclusionManagement(serverApplication)` line — that route is gone).

### 4.3 Then run the server tests

```powershell
.\gradlew.bat :server:test --console=plain
```

Expect real failures as well as compile fixes: where a test asserted on conclusion *identity* it may now need
`kb.commentsFor(case)`. Fix the test, never the production code, unless the production code is genuinely wrong — if you
think it is, stop and say so rather than changing it.

---

## 5. After the server module

### 5.1 `:ui`

```powershell
.\gradlew.bat :ui:compileTestKotlin --console=plain 2>&1 | Select-String "^e: "
```

- `ui/src/test/.../mocks/MockEngineDSL.kt` has an `ALL_CONCLUSIONS` route and a
  `returnConclusions` config field. Both are dead — the endpoint no longer exists. Delete the route branch and the
  field, and any test that used it.
- Test fixtures now take `commentTexts = listOf(...)` instead of
  `conclusionTexts = listOf(...)` (`createViewableCaseWithInterpretation`,
  `createLargeViewableCaseWithInterpretation`, `createCaseWithInterpretation`).

### 5.2 `:cucumber`

```powershell
.\gradlew.bat :cucumber:compileTestKotlin --console=plain 2>&1 | Select-String "^e: "
```

- `restclient/RESTClient.kt` — delete `getOrCreateConclusion`.
- `steps/Defs.kt` — uses `checkInterpretation`; convert as per recipe D.
- Then a dry run, which needs neither a server nor the LLM:
  `.\gradlew.bat :cucumber:cucumberDryRun`

### 5.3 Full verification

```powershell
.\gradlew.bat :common:test :server:test :ui:test --console=plain
.\gradlew.bat :cucumber:cucumberTest -Pfolder=chat
```

The known flake is the tooltip scenario "add a comment with a valid condition", which fails in a full chat-folder run
and passes alone. Anything else that fails is a real regression.

---

## 6. Finishing step 16

1. In `documentation/design/repeat_inferencing.md`, mark step 16 done, in the same style as steps 12–15, noting:
   conclusions retired; the `Definitions`
   directory added to export/import (this closed a real gap — a Phase-2 KB exported before this lost every comment);
   `Whatever.zip` converted;
   `ConclusionMigration` deleted; KB import now refuses a conclusion-era export. Record the one-off SQL for existing
   databases:
   `ALTER TABLE rules DROP COLUMN IF EXISTS conclusion;`,
   `DROP TABLE IF EXISTS conclusions;`, `DROP TABLE IF EXISTS conclusion_variables;`,
   `DROP TABLE IF EXISTS conclusion_indexes;`.
2. `server/src/test/resources/export/KBExported.zip` is referenced by no test (only the string "KBExported.zip" appears,
   as a multipart filename). Delete the file and say so in the summary.
3. Report to the user: what was converted, what changed shape and why, and the one remaining open question — whether the
   diff types (`Addition`/`Removal`/`Replacement`) should carry the comment attribute rather than only text. That was
   deferred until after step 16.

## 7. Rules of engagement

- **Never** weaken or delete an assertion to get a green build. If a test cannot be converted without losing its
  meaning, leave it failing and report it.
- **Never** change production code to make a test pass, unless the production code is genuinely wrong — in which case
  stop and explain.
- Do not add `relaxed = true` to `mockk` — it is set globally in
  `shared-test-resources/io/mockk/settings.properties`.
- Keep comments in the code explaining *why*, in the existing prose style; do not add narration of *what*.
- Commit nothing. The user commits.
