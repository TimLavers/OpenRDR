# Persistence

A knowledge base is held in memory while the server runs; persistence exists so that it survives a restart. The access
pattern shapes the design:

- the core items (attributes, conditions, rules, definitions, attribute order, description) are read once, when the KB
  is loaded, and written rarely;
- cases may number many thousands and must be interpreted quickly to show the effect of a proposed rule, so
  interpretation runs against in-memory objects, never against the store.

| Requirement          | Description                                                                                                                                                                                    | Validation                                                 |
|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| Two implementations  | Every store has an in-memory implementation, used by tests and the packaged demo, and a Postgres implementation, one database per KB named by the KB id.                                       | `persistence/inmemory/*Test`, `persistence/postgres/*Test` |
| Load at startup      | All stored KBs are loaded when the server starts.                                                                                                                                              | `KBManagerTest`                                            |
| Export format        | A KB exports as a ZIP of plain-text and JSON files, one directory per store, so that an archive is readable and diffable and can be checked into the test resources.                           | `KBExporterTest`, `KBImporterTest`                         |
| No in-code migration | Schema changes are made in the table definitions only. An existing database is migrated by hand with the SQL given when the change is made; there are no `ALTER TABLE` statements in the code. |                                                            |
| Single instance      | Within a loaded KB, equal objects are the same instance: every condition on `Glucose` refers to the one `Glucose` attribute, so a change to it is seen everywhere.                             | `ConditionManagerTest`                                     |
