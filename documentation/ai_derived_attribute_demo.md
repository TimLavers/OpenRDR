# Diabetes from clinical notes: demonstration

Run one visible scenario from the repository root:

```powershell
.\gradlew.bat :cucumber:aiDerivedAttributeDemo --console=plain
```

Use the existing Cucumber setup with its server, desktop and Gemini credentials.
`AI_EVALUATION_MODEL` optionally selects the runtime classification model when the
definition is created. By default it uses `GEMINI_MODEL` from `Gemini.kt`, the same
setting used by chat (currently `gemini-3.1-flash-lite`). The evaluation test uses this
same selection. Existing definitions retain the model saved in their evaluation profile.
The scenario opens a fresh default KB and uses synthetic notes only. It does not
import a prepared diabetes definition or use a scripted classifier.

The script creates `hasDiabetes` through chat, accepts the exact proposed definition,
and adds “The clinical notes indicate a personal history of diabetes.” with the
condition `hasDiabetes is "true"`. It then selects ten cases, checking both the
derived-value row and the comment, with four seconds to present each one.

| Case | Note | Expected display |
|---|---|---|
| Demo01 | Diabetic | true and comment |
| Demo02 | T2 Diab | true and comment |
| Demo03 | T1 diab | true and comment |
| Demo04 | FH diab | false, no comment |
| Demo05 | ?diabetes | false, no comment |
| Demo06 | gest diabetes | false, no comment |
| Demo07 | Explicitly negated diabetes, hypertension review | false, no comment |
| Demo08 | Family history plus confirmed personal type 2 diabetes | true and comment |
| Demo09 | Longer follow-up note mentioning longstanding T2DM | true and comment |
| Demo10 | Notes absent | Not evaluated, no comment |

The gestational exclusion is an explicit choice in this demo's definition. A false
value means these notes do not establish the defined personal history. It is not a
clinical conclusion that the patient does not have diabetes.

Hover over a successful result to show a two-column card with the source, approved
criteria and saved excerpt when one was returned. The final step revisits Demo08 and asks
chat to explain its saved result. Revisiting and explaining use the stored classification.

The six short notes are supplied examples. Demo07–09 demonstrate handling of wording
that was not supplied as examples; they are not a statistical accuracy evaluation.
The live scenario asserts the expected values and fails visibly on disagreement.
The dry run verifies bindings only and does not establish that the real models pass.

Optional additions for a longer presentation:

- Change the definition globally to include gestational diabetes and revisit Demo06;
  the new revision obtains a new evaluation.
- Make a case-specific literal override and show that ordinary RDR exceptions still work.
- Compare a note with both family history and personal diabetes against family history alone.
- Show a service failure and retry in chat; a failure is displayed as incomplete and cannot
  generate a report or masquerade as a negative classification.

Export/import was implemented separately as the final stage. Its Cucumber scenario is
`cucumber/src/test/resources/requirements/inferencing/AI evaluation export import.feature`,
tagged `@ai_evaluation_export`. It compares true/false values, downstream comments and full
saved provenance after importing the archive. It is separate from the ten-case demo.

## Headless chat workflow check

The opt-in `AiDefinitionConversationModelTest` checks the authoring conversation with
the configured Gemini chat model. It proposes a definition, accepts it, verifies that
the server asks for reasons while the rule is pending, then declines reasons and checks
that the rule is committed. It uses made-up notes in an in-memory KB and a fixed
classifier; it checks the chat workflow, not classification quality. It needs `API_KEY`
and does not open the desktop UI.

```powershell
$env:RUN_AI_AUTHORING_TEST = 'true'
try {
    .\gradlew.bat :server:cleanTest :server:test --tests 'io.rippledown.kb.chat.AiDefinitionConversationModelTest' --console=plain
} finally {
    Remove-Item Env:RUN_AI_AUTHORING_TEST
}
```

## Separate model evaluation

The opt-in `AiDiabetesEvaluationTest` uses the six supplied examples, twelve held-out
synthetic notes (including long, contradictory and adversarial inputs), and repeated
evaluations of a subset. It uses fresh Gemini requests, without reusing saved classifications,
and never writes to a KB.

```powershell
$env:RUN_AI_DERIVED_EVALUATION = 'true'
.\gradlew.bat :llm:test --tests 'io.rippledown.llm.AiDiabetesEvaluationTest' --rerun-tasks --console=plain
Remove-Item Env:RUN_AI_DERIVED_EVALUATION
```

Reports under `llm/build/reports/ai-evaluation` record the full definition/profile,
revision, expected and actual labels, evidence, returned model version, elapsed time,
failure and repetition. The six supplied examples must pass. Review all held-out
disagreements before adopting a profile; no clinical error threshold is assumed.
