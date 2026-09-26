# OpenRDR documentation

OpenRDR is an open-source implementation of Ripple-Down Rules (RDR), a knowledge-acquisition method in which a domain
expert corrects the interpretation of one case at a time and the system turns each correction into a rule. The
canonical domain is chemical pathology: a case is a set of test results for a patient, the interpretation is a set of
comments, and the expert is a pathologist.

## What makes RDR different

A rule is never edited. When the interpretation of a case is wrong, the expert says what should change (add, remove or
replace a comment) and why (conditions true for this case). The system then shows the **cornerstone cases**, the cases
on
which earlier rules were built and whose interpretation the new rule would also change. For each one the expert either
accepts the change or adds a condition that excludes that case. The refinement is stored as a child of the rule it
corrects, so knowledge only ever grows and every rule remains justified by the case it was built on.

OpenRDR's distinctive choice is that the expert does all of this through a chat, in their own words and language, and
that the language model is confined to *understanding* the expert: every change to the knowledge base is derived and
validated by deterministic server code. See [design/chat_architecture.md](design/chat_architecture.md).

## How the documentation is organised

| Folder          | Contents                                                                                                                                                                                                                                                  |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `requirements/` | What the system does and why, one file per capability area. Each row of a requirements table names the cucumber feature that pins it; the `.feature` files under `cucumber/src/test/resources/requirements/` are the executable form of the requirements. |
| `design/`       | How each mechanism works and why it was built that way, including the alternatives rejected. Design docs name entry-point classes only; details live in the code and its tests.                                                                           |
| `reviews/`      | External reviews of the design, kept verbatim.                                                                                                                                                                                                            |
| `tickets/`      | Open work: the backlog and plans for features not yet built.                                                                                                                                                                                              |

### Requirements

- [Cases and attributes](requirements/cases_and_attributes.md)
- [Comments](requirements/comments.md)
- [Derived attributes](requirements/derived_attributes.md)
- [Conditions](requirements/conditions.md)
- [Rule building](requirements/rule_building.md)
- [The chat](requirements/chat.md)
- [Knowledge bases](requirements/knowledge_bases.md)
- [AI report generation](requirements/ai_report_generation.md)
- [Persistence](requirements/persistence.md)

### Design

- [Architecture](design/architecture.md): modules, server and client, how the client learns of server changes.
- [Rule tree and inference](design/rule_tree_and_inference.md): derived attributes, comments as attributes, fixpoint
  inference, cycle prevention.
- [Derived attribute definitions](design/derived_attribute_definitions.md): why the formula lives on the attribute.
- [Comments](design/comments.md): naming, variables, showing conditions in the user's words.
- [Conditions](design/conditions.md): the evaluation model and the treatment of real numbers.
- [Condition translation](design/condition_translation.md): natural language to `Condition`.
- [Suggested conditions](design/suggested_conditions.md): generation, ranking and editing.
- [Chat architecture](design/chat_architecture.md): the conversation, actions, the rule-building workflow.
- [Chat UI guidelines](design/chat_ui_guidelines.md): when an inline control is allowed.
- [Knowledge base management](design/kb_management.md): by chat, demonstrations, import and export.
- [Previewing pending changes](design/previewing_pending_changes.md): what the panels show during a rule session.
- [AI report generation](design/ai_report_generation.md)
- [Voice input](design/voice_input.md)
- [Testing](design/testing.md): acceptance tests, page objects, OCR.
- [Decisions log](design/decisions_log.md): one line per decision, with a link.

## Working conventions

- Design docs are updated when a decision is resolved or a feature lands, in the present tense. A doc describes the
  system as it is; the history is in git.
- Requirements that are wanted but not built are marked **not implemented** in the requirements doc, and planned work
  lives in `tickets/`.
- Build and test commands are in the project rules under `.windsurf/rules/`.

## Background reading

The first knowledge base implemented the rules in Vasikaran and Loh, *Interpretative commenting in clinical chemistry
with worked examples for thyroid function test reports*, Practical Laboratory Medicine 26 (2021), included as
[requirements/Interpretative_commenting_in_clinical_chemistry_wi.pdf](requirements/Interpretative_commenting_in_clinical_chemistry_wi.pdf).
The Zoo knowledge base follows Compton and Kang, *Ripple-Down Rules: The Alternative to Machine Learning*, CRC Press

2021. Third-party code credits are in [credits.md](credits.md).
