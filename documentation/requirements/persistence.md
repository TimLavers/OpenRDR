# Persistence Requirements
OpenRDR `KB`s are in memory during operation (case interpretation and rule building).
A persistence system is needed so that `KB`s survive JVM shutdown. The general requirements
around this are:
- reads of the `KB` core items (`Attribute`s, `Conclusion`s, `Condition`s and `Rule`s) only occur when a `KB` is loaded.
- writes of the `KB` core items are also quite infrequent.
- a `KB` may have many thousands of associated `RDRCase`s , which may need to be interpreted quickly to see the effects of proposed rule changes.

