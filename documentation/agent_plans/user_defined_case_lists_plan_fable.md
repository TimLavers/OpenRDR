# User-defined case lists (fable)
Replace the single built-in "favourites" list with any number of named, user-created case lists, per `documentation/requirements/user_defined_case_lists.md` and `cucumber/src/test/resources/requirements/cases/User defined case lists.feature`, and remove the favourites feature (`documentation/requirements/favourite_cases.md`).
## Current state
* `enum class CaseType { Cornerstone, Processed, Favourite }` (`common/src/main/kotlin/io/rippledown/model/CaseId.kt:5`); `CaseId.type` defaults to `Processed`. `RDRCase.copyWithoutId`/`copyWithNewNameAndNoId`/`RDRCaseBuilder.setCaseType` take a `CaseType`.
* `CasesInfo` has `favouriteCaseIds: List<CaseId>`; `count` omits favourites — a latent bug once a case can live only in a user list, because the UI gates the whole selector and chat context on `count > 0`.
* Two places build `CasesInfo` and both must change: `KBEndpoint.waitingCasesInfo()` (`server/.../server/KBEndpoint.kt:67-72`, REST initial load) and `RuleSessionManager.casesInfo()` (`server/.../kb/RuleSessionManager.kt:993-998`, websocket pushes).
* `KB.kt:98-112`: `favouriteCaseIds()`, `copyCaseAsFavourite(id, newName?)`, `deleteCaseFromFavourites(case)`. `CaseManager.ids(type)`/`all(type)` are already generic.
* `PostgresCaseStore` persists the type as its **enum ordinal** (`PostgresCaseStore.kt:27,101,119`); `InMemoryCaseStore` is type-agnostic.
* Wire/disk JSON: `CaseId` serializes the type as the bare name (`"type":"Cornerstone"`). There are no existing KB exports or databases to stay compatible with (decided), so no migration constraints apply.
* Chat: `RuleService.copyCaseToFavourites/deleteCaseFromFavourites` (`RuleService.kt:123-124`), implemented at `RuleSessionManager.kt:981-991`; action classes `CopyCaseToFavourites`, `CopyCaseToFavouritesWithNewName`, `DeleteCaseFromFavourites` instantiated reflectively by simple class name from `ActionComment.createActionInstance()`; constants `COPY_CASE_TO_FAVOURITES` etc. in `common/.../constants/chat/Constants.kt:84-86`; prompt vars in `KBChatService.kt:236-238` plus section file `25_favourite_cases.md` listed at `KBChatService.kt:164`; "Favourite cases" capability section in `ListCapabilities.kt:59-65`.
* UI: `CaseSelector.kt` renders exactly one "Favourites" section from a `favouriteCaseIds` parameter with fixed `FAVOURITES_SECTION_ID`/`FAVOURITES_SECTION_HEADER_ID`; `OpenRDRUI.kt` reads `casesInfo.favouriteCaseIds` at lines 184, 224 and 311 (combined-id ordering, chat context, selector call).
* Cucumber: fixed `FavouriteCaseListPO`/`FavouriteCaseCountPO` and accessors (`RippleDownUIOperator`, `LaunchedClient`, `StepsInfrastructure`); favourites-specific steps in `CaseListStepDefs.kt:21-57` and `ChatCaseDefs.kt`. The new feature file is already `{string}`-parameterised but nothing binds those steps yet, and lines 52 and 96 still use the old unquoted wording — fix them.
* Export: `KBExporter` writes only `CornerstoneCases` + `ProcessedCases`, so favourites are silently dropped today despite `favourite_cases.md` claiming otherwise; including user lists in exports is new work.
## Design decisions
* `CaseListType` replaces the enum: `common/.../model/CaseListType.kt`, a small class holding `name: String` with a **custom `KSerializer` that writes/reads the bare name string** — the simplest JSON shape. Built-ins: `CaseListType.Processed` ("Processed") and `CaseListType.Cornerstone` ("Cornerstone").
* `isBuiltIn` property; companion `isReservedListName(name)` normalises (trim, lowercase, drop a trailing " cases"/" case list", accept "cornerstones") so copying into "Cornerstone Cases", "processed" etc. is refused. A user list exists exactly while at least one case carries its name (no registry to persist); list names are **case-insensitive** (decided): `CaseListType.equals`/`hashCode` use the lowercased name while `name` keeps the entered spelling for display and serialisation, so grouping by `CaseListType` merges "good" and "Good" automatically; Stage 2's `KB.copyCaseToList` resolves a target name to the existing list's spelling so a list shows one spelling.
* `CasesInfo.favouriteCaseIds` becomes `userDefinedCaseLists: List<CaseListInfo>` with `@Serializable data class CaseListInfo(val name: String, val caseIds: List<CaseId>)`; lists ordered by their lowest case id (creation order), cases within a list by id (copy order — feature: "listed in the order in which they were added"). Fix `count` to include them. `KB.userDefinedCaseLists()` builds the grouping once; both `KBEndpoint.waitingCasesInfo()` and `RuleSessionManager.casesInfo()` use it.
* Chat actions: **one** `CopyCaseToList(listName: String, newName: String? = null)` replaces both copy actions — `ActionComment.invokeConstructor` already skips absent optional parameters and `newName` is already an `ActionComment` field, so both JSON shapes bind to the one class; add a `listName` field. `DeleteCaseFromList()` stays parameterless (the current case knows its own list). Built-in-name refusal is server-side and deterministic: `KB.copyCaseToList` throws `IllegalArgumentException`; the action catches it and returns its message as the `ChatResponse` (pattern of `RenameAttribute.kt:27-31`). Message contains "Cannot" and the offending name, satisfying the feature's `Cannot`/`Cornerstone` terms.
* Section accessibility ids come from shared builders in `constants.caseview` (`userListSectionId(name)` / `userListSectionHeaderId(name)`), used by both the UI and the cucumber page objects so they cannot drift.
* Export adds a `UserDefinedCases` directory; each exported case already records its list in `CaseId.type`, so import needs no extra bookkeeping.
## Stage 1: Common model
TDD throughout: failing test first per change.
* New `CaseListType` (+ serializer) with tests: round-trip (spelling preserved), bare-name JSON shape, case-insensitive `equals`/`hashCode`, `isBuiltIn` (including case variants), `isReservedListName` variants.
* `CaseId.type: CaseListType = CaseListType.Processed`; delete the enum; adjust `RDRCase.kt` (builder `setCaseType`, `copyWithoutId`, `copyWithNewNameAndNoId`).
* `CasesInfo`: add `CaseListInfo`, replace `favouriteCaseIds`, fix `count`.
* Update `CaseIdTest`, `CasesInfoTest`, `RDRCaseTest`, `RDRCaseDataTest`, testFixtures `TestUtils.kt`. Gate: `gradle :common:test`.
## Stage 2: Server core and persistence
* `CaseManager`: parameter type becomes `CaseListType`; add grouping support for `KB.userDefinedCaseLists()`.
* `KB`: replace the three favourites methods with `userDefinedCaseLists(): List<CaseListInfo>`, `copyCaseToList(id: Long, listName: String, newName: String?)` (throws for reserved names; blank `newName` keeps the original name, as now), `deleteCaseFromUserList(case)` (throws for built-in types), `allUserDefinedCases()` (for export).
* `KBEndpoint.waitingCasesInfo()` uses the new shape.
* `PostgresCaseStore`: `PGCaseIds.type` int → `varchar` holding `CaseListType.name`. No existing databases, so no migration concerns.
* Update `KBTest.kt:827-909`, `PostgresCaseStoreTest`, `InMemoryCaseStoreTest`, `WebSocketManagerTest`. Gate: `gradle :server:test` (postgres package needs a local database).
## Stage 3: Chat layer
* `RuleService`/`RuleSessionManager`: `copyCaseToList(case, listName, newName?)`, `deleteCaseFromUserList(case)`; `casesInfo()` built from `kb.userDefinedCaseLists()`. Update `RuleSessionManagerTest.kt:1122-1313`.
* Delete the three favourites action classes and tests; add `CopyCaseToList` and `DeleteCaseFromList` with tests (copy, copy-with-new-name, reserved-name refusal → `ChatResponse`, delete).
* `ActionComment`: add `listName` field and wire it into `invokeConstructor`; test in `ActionCommentTest`.
* Constants: `COPY_CASE_TO_LIST`/`DELETE_CASE_FROM_LIST` replace the three favourites constants (values = class simple names, required by the reflective lookup); update `KBChatService.systemPromptVariables` and rename/rewrite `25_favourite_cases.md` → `25_user_defined_case_lists.md` (both JSON shapes for `CopyCaseToList`, list name taken verbatim from the user, no model-side refusals — the server refuses reserved names).
* `ListCapabilities.kt:59-65` → "User-defined case lists" section; update `ListCapabilitiesTest` and note ui `CapabilityCardTest` for Stage 4.
## Stage 4: UI
* `constants.caseview/Constants.kt`: remove the two fixed favourites ids; add `userListSectionId(name)`/`userListSectionHeaderId(name)`.
* `CaseSelector`: replace `favouriteCaseIds` with `userDefinedCaseLists: List<CaseListInfo>`; render a `CollapsibleSectionHeader` ("<name> (n)") + `CaseSectionList` per list; per-name expand state (`mutableStateMapOf`, default expanded); focus-requester order = processed, cornerstones, then each user list in order; `requestFocusOnCase` composed-check generalised.
* `OpenRDRUI.kt:184,224,311`: use `casesInfo.userDefinedCaseLists` (flattened for `lastKnownCaseOrder` and chat-context membership).
* Update `CaseSelectorTest`, `CaseSelectorLayoutTest`, `CaseSelectorHeaderTest`, `CaseSelectorProxy`, `OpenRDRUITest`, `CapabilityCardTest`. These are UI unit tests — at most one to be run without asking; permission to be requested for the suite.
## Stage 5: Cucumber
* Feature file: quote the list name on lines 52 and 96 to match the parameterised step.
* Replace `FavouriteCaseListPO`/`FavouriteCaseCountPO` with `UserCaseListPO(name)`/`UserCaseCountPO(name)` built on the shared id helpers; add a section-hidden assertion to `AbstractCaseSectionListPO`/`AbstractCaseCountPO` for `I should no longer see the {string} case list`.
* Parameterised accessors through `RippleDownUIOperator`/`LaunchedClient`/`StepsInfrastructure` (`userCaseListPO(name)`, `userCaseCountPO(name)`).
* Steps: `the {string} case list should contain:`, `I select case {word} on the {string} case list`, `I should no longer see the {string} case list` (`CaseListStepDefs`); `I copy the current case to the {string} case list` / `... with name {string}` / `I delete the current case from the {string} case list` (`ChatCaseDefs`), with chat text quoting the list name so the model extracts it verbatim.
* Gates: `gradle :cucumber:compileTestKotlin`, then `gradle :cucumber:cucumberDryRun`. Live scenarios need server + LLM + GUI — to be run by Tim, or with his explicit go-ahead.
## Stage 6: Export/import
* `KBExportImport`: add `userDefinedCasesDirectory` ("UserDefinedCases"); `KBExporter.export()` writes `kb.allUserDefinedCases()` there; `KBImporter.import()` reads it — no legacy zips to consider, so the directory is always written and read.
* Round-trip test: a case copied to a named list survives export → import with list name and order intact (no favourites export test exists to adapt).
## Stage 7: Cleanup and verification
* Delete `documentation/requirements/favourite_cases.md`; update the favourites references in `documentation/requirements/case_management.md` and `documentation/design/chat_ui_guidelines.md:27`.
* Repo-wide grep for `[Ff]avourite|FAVOURITE` should return nothing.
* Final gates in order: `:common:test`, `:server:test`, `:cucumber:compileTestKotlin`, `:cucumber:cucumberDryRun`; live cucumber runs on Tim's signal.
## Open questions
* ~~List-name case sensitivity~~ Resolved: list names are case-insensitive (see Design decisions).
* ~~Existing dev databases and KB exports~~ Resolved: there are none to consider; no migration or legacy-parse provisions needed.
