---
trigger: always_on
---

# Persistence and schema changes

- No in-code database migrations. Do not put `ALTER TABLE` statements in a store's `init` block.
- A new column is simply part of the Exposed table definition, so `SchemaUtils.create` builds it for a fresh database.
- For an existing configured database the user runs the migration by hand. Whenever the schema changes, give them the
  one-off SQL statements to run, and say that they should reconfigure afterwards.
