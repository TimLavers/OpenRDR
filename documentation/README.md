# OpenRDR documentation

OpenRDR implements Ripple-Down Rules (RDR): an expert corrects one case's interpretation and supplies reasons.
The system checks the proposed rule against cornerstone cases before committing it. Rules can assign comments
or derived values, and later rules can use those values as conditions.

Chat translates intent into server-validated operations. AI Boolean definitions also use a model during inference
to classify primary text against an approved definition; successful results are saved for reuse. Generated reports
summarise rule-authored comments. The designs below explain these distinct roles.

## Start here

- [Project setup](../README.md)
- [Architecture](design/architecture.md)
- [Testing and commands](design/testing.md)
- [Packaged demonstration](../packaging/README-demo.txt)
- [AI-derived attribute demonstration and model evaluation](ai_derived_attribute_demo.md)

## Requirements

Requirements describe current behaviour and identify unimplemented capabilities. Validation references name unit
tests or feature files beneath `cucumber/src/test/resources/requirements/`.

- [Cases and attributes](requirements/cases_and_attributes.md)
- [User-defined case lists](requirements/user_defined_case_lists.md)
- [Comments](requirements/comments.md)
- [Derived attributes, including AI Boolean definitions](requirements/derived_attributes.md)
- [Conditions](requirements/conditions.md)
- [Rule building](requirements/rule_building.md)
- [Chat](requirements/chat.md)
- [Knowledge bases and awareness of other users](requirements/knowledge_bases.md)
- [AI report generation](requirements/ai_report_generation.md)
- [Persistence](requirements/persistence.md)

## Design

- [Architecture](design/architecture.md): modules and client/server responsibilities.
- [Rule tree and inference](design/rule_tree_and_inference.md): assignments, fixpoint inference and cycle checks.
- [Derived attribute definitions](design/derived_attribute_definitions.md): defaults and rule-specific overrides.
- [AI-derived attributes](design/ai_derived_attributes.md): authoring, evaluation, persistence and failures.
- [Comments](design/comments.md): templates, variables and condition phrases.
- [Conditions](design/conditions.md): predicates, episodes and numeric comparisons.
- [Condition translation](design/condition_translation.md): natural language to validated conditions.
- [Suggested conditions](design/suggested_conditions.md): generation, ranking and editing.
- [Chat architecture](design/chat_architecture.md): protocol, conversations and rule-building workflow.
- [Chat UI guidelines](design/chat_ui_guidelines.md): inline controls and passive displays.
- [Knowledge base management](design/kb_management.md): naming, demonstrations and import/export.
- [Concurrent users](design/concurrent_users.md): locks, sessions, presence, change history and recovery.
- [Previewing pending changes](design/previewing_pending_changes.md)
- [AI report generation](design/ai_report_generation.md)
- [Voice input](design/voice_input.md)
- [Testing](design/testing.md)
- [Shared Cucumber server](design/shared_cucumber_server.md): reset, isolation and process lifecycle.
- [Decisions log](design/decisions_log.md)

## Maintenance and background

- [Backlog](tickets/TODO.md): outstanding work, without completed implementation plans.
- [Coding and execution instructions](../AGENTS.md)
- [Historical design review, August 2026](reviews/design_review_2026-08.md): retained as review context;
  it describes an earlier architecture and is not the current specification.
- [Third-party credits](credits.md)

The thyroid sample follows Vasikaran and Loh, *Interpretative commenting in clinical chemistry with worked
examples for thyroid function test reports*, Practical Laboratory Medicine 26 (2021), included as
[the reference paper](requirements/Interpretative_commenting_in_clinical_chemistry_wi.pdf).
The Zoo sample follows Compton and Kang, *Ripple-Down Rules: The Alternative to Machine Learning*, CRC Press, 2021.

Update requirements and design when behaviour changes. Keep current behaviour in the present tense and unfinished
work in the backlog. Historical implementation steps and transient validation logs belong in Git history or build
artifacts rather than the maintained design.
