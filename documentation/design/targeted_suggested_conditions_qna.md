# Targeted Suggested Conditions — design Q&A

Follow-up discussion to `targeted_suggested_conditions.md`, focused on Phase 3
(LLM-assisted re-ranking / generation) and whether the condition model should
be refactored to use a general AST. Consolidated from a pair-programming
session on 2 May 2026.

---

## Q1. Can we take Phase 3 further?

> I like all the staged approaches. On point 4 (LLM-assisted re-ranking /
> generation), I'm wondering if we could take this a step further. What
> about if the model comes up with a free-form condition expression (i.e.
> evaluating as a boolean), but then, if it is not equivalent to an
> existing condition, creates a condition class (or function) that
> implements its condition expression, compiles it, adds it to the
> "condition library", provides a unit test, and then it can be
> subsequently reused. Possible? Desirable?

### Short answer

**Possible**, but less straightforward than it sounds, and **only
partially desirable**. Most of the benefit is achievable without
compiling or classloading anything. Suggested split: the expression-level
version in Phase 3, the codegen version only if/when Phase 3 shows clear
wins.

### The constraint the current code imposes

`common/src/main/kotlin/io/rippledown/model/condition/Condition.kt`:

```kotlin
@Serializable
sealed class Condition {
    abstract val id: Int?
    abstract fun holds(case: RDRCase): Boolean
    abstract fun asText(): String
    ...
}
```

`Condition` is a **sealed class** with kotlinx.serialization. That has three
hard consequences for "the LLM generates a new condition class and installs
it":

- **Sealed → closed at compile time.** Adding a new subclass at runtime
  isn't possible without dynamic classloading *and* re-registering the
  polymorphic serializer. KBs must survive restarts, so the new subclass
  needs to be loadable in every subsequent process — not just the one
  that generated it.
- **Identity and dedup go through `sameAs`**
  (`server/src/main/kotlin/io/rippledown/kb/ConditionManager.kt`). Any new
  class has to define "equivalent", or it gets duplicated every time the
  LLM paraphrases the same logic.
- **KB portability**: a KB is supposed to move between machines and
  releases. A KB that references compiled bytecode generated on a
  different machine at a different time is a serious distribution
  problem — every consumer of that KB must be able to rebuild or retrieve
  the exact class.

### The less ambitious variant (recommended)

Most of the value comes from adding a **single new `Condition` subclass**,
once at compile time — an `ExpressionCondition`:

```kotlin
@Serializable
data class ExpressionCondition(
    override val id: Int?,
    val expression: String,
    val attrRefs: List<AttributeRef>
) : Condition() {
    override fun holds(case: RDRCase): Boolean =
        Interpreter.eval(expression, case, attrRefs)
    override fun asText(): String = expression
    override fun sameAs(other: Condition) =
        other is ExpressionCondition && other.expression == this.expression
    ...
}
```

LLM step becomes:

- Propose condition as expression text in the existing DSL (the existing
  `ConditionExpressionParser` already parses a fixed vocabulary).
- Parse → validate → if `sameAs` existing, reuse; otherwise persist a new
  `ExpressionCondition` via `ConditionManager.getOrCreate`.
- **No compilation, no classloading, no codegen, no test generation.**

This gives:

- Reuse across rules (same expression text → `sameAs` → one persisted row).
- Readable provenance (the text *is* the condition).
- Trivial KB portability (it's just a string).
- Safe evaluation (DSL, not arbitrary Kotlin).
- Mockability for tests (the `setConditionParser` pattern extends naturally).

The only thing this *doesn't* give that "compile a class" does: native-JVM
execution speed for very hot condition sets, and access to arbitrary Kotlin
operators. Neither appears to be the bottleneck here.

### The full codegen variant — what it would take

- **Kotlin scripting / embeddable compiler** dep (`kotlin-scripting-jvm-host`,
  ~10–15 MB). Non-trivial dep, real start-up cost.
- **A sandbox / allowlist** for what generated code can do. The LLM will,
  sooner or later, emit code that reads files or opens sockets; the server
  must refuse to compile anything outside a well-defined surface. This is
  the hardest part.
- **Persistence as source text, not bytecode.** Ship `.kts`/`.kt` sources in
  the KB, re-compile on load. Compiling on every KB open is slow; a local
  compile cache keyed on source hash is mandatory.
- **Versioning**: if the interpreter-facing API the LLM used ever changes,
  every generated class breaks. Need a versioned "stable surface for
  generated conditions" — a facade.
- **LLM-generated unit tests.** The proposed test belongs *with* the
  condition and must pass before install. This is a nice correctness gate,
  but the tests are also LLM-generated: they assert what the model *thought*
  the condition meant, not what the human needed. Their signal is "the code
  the model produced matches the code the model described", which is
  weaker than it looks. They do catch parse/compile errors and obvious
  crashes.
- **Classloader management**: per-class classloaders so you can evict/replace
  on re-generation without leaking; sealed hierarchies and
  kotlinx-serialization polymorphism both become fiddly.
- **Operational debuggability**: a stack trace pointing into
  `GeneratedCondition_a3f29.kt:14` five months from now is much harder to
  diagnose than an expression string.

Substantial feature — months of work — and most of it is *infrastructure*,
not AI.

### Where the compile-and-install variant would genuinely win

- The condition needs expressive power beyond a declarative DSL (loops,
  recursion, calls into domain helpers).
- You need native-JVM execution speed for cases evaluated millions of times.
- You want to ship the condition as a reusable *named* library primitive
  across KBs, with a stable human-authored API — i.e. the LLM is promoting
  a condition to the same status as hand-written ones in
  `common/src/main/kotlin/io/rippledown/model/condition/`.

None of that looks urgent.

### Suggested refinement to the staged plan

Keep Phases 1–2 as drafted. Reshape current Phase 3 into two sub-phases:

- **Phase 3a — LLM + expression persistence (simple variant).**
  Add `ExpressionCondition` to the sealed hierarchy. LLM proposes expression
  text, parse it, check `sameAs`, persist via `ConditionManager.getOrCreate`.
  Smallest change that gets novel, reusable, free-form conditions without
  new runtime infrastructure.
- **Phase 3b — Dynamic class generation (speculative).**
  Only pursue if Phase 3a usage data shows (a) expression evaluation is a
  measurable bottleneck, or (b) users want operators the DSL genuinely
  can't express. At that point, design the sandbox, versioned facade, and
  test-gen story properly rather than bolting them on. **YAGNI** until then.

### Two risks worth naming in the design doc regardless of variant

- **Canonicalisation of expressions.** Without it,
  `x > 10 && y < 5` and `y < 5 && x > 10` persist as two different
  conditions. Pick a canonical form (text, or parsed) before `sameAs` or
  the library fragments.
- **LLM-authored tests don't validate intent.** Worth calling out so future
  readers don't mistake "tests pass" for "condition is correct".

---

## Q2. What did you mean by "the interpreter evaluates `holds(case)` from the parsed AST"?

### What an AST is, in general

**AST** = abstract syntax tree. A tree data structure where each node
represents one grammatical element of an expression.

Given condition expression text, an AST-based approach is:

1. **Lex** the text into tokens (`TSH`, `>=`, `4.0`, `AND`, `(`, ...).
2. **Parse** the tokens into a tree. For `TSH >= 4.0 AND Free T4 is low`:
   ```
   And
   ├── Comparison(attribute="TSH", op=">=", value=4.0)
   └── Predicate(attribute="Free T4", kind=Low)
   ```
3. **Persist** the AST alongside the text (or re-parse on load — the text
   is the canonical serialised form).
4. **Evaluate** `holds(case)` by walking the tree:
   ```kotlin
   fun eval(node: Node, case: RDRCase): Boolean = when (node) {
       is And        -> eval(node.left, case) && eval(node.right, case)
       is Or         -> eval(node.left, case) || eval(node.right, case)
       is Not        -> !eval(node.inner, case)
       is Comparison -> compare(case.getLatest(node.attr), node.op, node.value)
       is Predicate  -> node.kind.matches(case.getLatest(node.attr))
   }
   ```

The "interpreter" is just that `eval` function. **Nothing is compiled.**
Plain Kotlin traversing a data structure. Same idea as a SQL engine walking
a query plan, or `RegExp.matcher(...)` walking a compiled regex.

### But the current OpenRDR parser isn't really AST-based

`server/src/main/kotlin/io/rippledown/server/ConditionExpressionParser.kt` is
a flat list of ~15 regex patterns. Each successful match returns a
**fully-formed existing `Condition` subclass instance**
(`EpisodicCondition`, `CaseStructureCondition`, `SeriesCondition`). No tree,
no composition, no `AND`/`OR`/parens. A match like `TSH is high` becomes
exactly one `EpisodicCondition(TSH, High, Current)`.

So for today's vocabulary, "interpret the AST" degenerates to **"call
`holds(case)` on the concrete subclass the parser produced"**. No interpreter
needed beyond what the sealed hierarchy already provides. The earlier
phrasing was unnecessarily grand.

### Where a real AST starts to matter

Only when the grammar gains compositional structure the current types
can't express:

- Boolean combinations: `TSH is high AND Free T4 is low`
- Negation: `NOT (Free T4 is normal)`
- Cross-attribute comparisons: `TSH > Free T4 * 2`
- Parenthesised grouping for precedence.

At that point one new subclass — `ExpressionCondition(expression: String)` —
whose `holds(case)` parses the text into a tree (or uses a cached tree) and
evaluates by tree-walking, gives you the needed power.

### Three designs, cheapest to most expensive

1. **Today**: parser text → existing subclass instance → its own `holds`.
   No AST, no interpreter. Works for a fixed vocabulary.
2. **Expression-text condition with tree-walking interpreter**: one new
   subclass, richer grammar, recursive evaluator in plain Kotlin. No code
   generation, no classloader work, no test generation. KB artefact is
   just the expression string. *Phase 3a from Q1.*
3. **LLM-generated compiled Kotlin class**: source → `kotlinc` → classloader
   → register in sealed hierarchy and serializer polymorphism →
   LLM-generated tests. *Phase 3b from Q1; significant infrastructure.*

Design 2 gives most of what design 3 promises at a fraction of the
complexity. Design 3 only pulls ahead if you need execution speed or
grammar power beyond an interpreter (loops, helper calls, etc.).

---

## Q3. Should our existing conditions be refactored as ASTs?

### Short answer

**No** — not as a refactor of the existing ones. The current `Condition`
hierarchy is already doing the same job an AST would do, just with a
schema tuned to the RDR domain. An AST only earns its keep when you need
**user-authored compositional expressions**, and when you get there it
should live *alongside* the existing conditions as one more subclass, not
replace them.

### The current design is already "AST-like"

An AST is "a tree of typed nodes whose types correspond to the grammar,
with an evaluator that recurses over them." What already exists:

```kotlin
// common/.../condition/Condition.kt
@Serializable
sealed class Condition {
    abstract fun holds(case: RDRCase): Boolean
    abstract fun asText(): String
    ...
}

// common/.../condition/EpisodicCondition.kt
data class EpisodicCondition(
    override val id: Int? = null,
    val attribute: Attribute,
    val predicate: TestResultPredicate,
    val signature: Signature,
    ...
) : Condition() {
    override fun holds(case: RDRCase): Boolean {
        val values = case.values(attribute) ?: return false
        return signature.matches(values.map { predicate.evaluate(it) })
    }
}
```

That *is* a two-level AST:

- Root node: `EpisodicCondition` with slots `attribute`, `predicate`,
  `signature`.
- Child nodes: `TestResultPredicate`, `Signature` — themselves sealed
  hierarchies with their own `evaluate` / `matches` methods.
- `holds(case)` is the interpreter; it recurses into children.

Same for `SeriesCondition` (attribute + `Trend`) and
`CaseStructureCondition` (structural predicate).

The difference from a "general-purpose AST" is that the grammar is
**fixed and domain-specific**: each root subclass corresponds to one of
three well-understood RDR condition shapes, not to a generic
`And`/`Or`/`Comparison` algebra. Benefits preserved by this specialisation:

- **Structurally self-describing** — UI editors can switch on subclass.
- **Stable serialised form** — kotlinx polymorphism handles it.
- **Meaningful `sameAs`** — compare typed slots directly; no need to
  normalise arbitrary boolean algebra.
- **Trivial persistence** — tree is at most two levels deep with known
  shapes.

A refactor to a uniform general AST would lose all four in exchange for
uniformity you don't currently need.

### The distinction that matters

Two questions masquerade under "AST":

1. **Is the condition model a tree of typed nodes with an evaluator?**
   Yes — OpenRDR already has this. Nothing to refactor.
2. **Is it a *generic, compositional* AST over arbitrary boolean
   expressions?** No, and shouldn't be for the existing conditions.
   Those have specific clinical semantics (`AtLeast(3) Free T3 are
   Increasing`) better captured by dedicated node types than by a generic
   algebra.

The `ExpressionCondition` suggestion in Q1 is purely about question 2 —
and only as an *additional* branch of the sealed hierarchy, not a
refactor of the existing branches:

```
Condition (sealed)
├── EpisodicCondition        ← unchanged, already AST-shaped for its domain
├── SeriesCondition          ← unchanged
├── CaseStructureCondition   ← unchanged
└── ExpressionCondition      ← new: holds a parse tree of And/Or/Not/Compare
```

### When a refactor *would* be justified (none of these apply today)

- You want **algebraic manipulation**: detect that one rule's condition
  implies another's, simplify contradictions, or normalise to canonical
  form across all condition types. That needs a uniform representation.
- You want to express conditions the current types can't:
  `(TSH is high AND Free T4 is normal) OR TSH > 2*Free T4`.
- You want to **eliminate the distinction** between structural, episodic,
  and series conditions because downstream consumers are tired of the
  switch.

None are in evidence. Existing specialisation is pulling its weight — user-
facing text, dedicated editors, serialisation, `sameAs`, and targeted
suggestion generation all benefit from knowing the condition *kind*.

### Recommendation

- **Don't refactor.** `EpisodicCondition`, `SeriesCondition`,
  `CaseStructureCondition` are doing the right job.
- **If and when** free-form expressions are worth it (Phase 3a), **add**
  `ExpressionCondition` alongside them. That subclass internally holds a
  parse tree (the "AST" in the strict sense), with a small interpreter in
  `holds(case)`. The other three never need to know it exists.
- You'll then want one UI helper that can render `ExpressionCondition` as
  text (round-tripping through its source expression), while the existing
  three keep their bespoke editors.

AST as a concept is already present, done well, at the right scope.
Generalising it is a tool for a specific future problem (compositional
free-form expressions); keep the tool in the drawer until that problem
arrives.
