# Coding Instructions

## Production Kotlin

**Guidelines for Production code**

- follow TDD as far as possible - write a failing unit test before production code
- All new classes to be in kotlin
- Major functionality in existing java class also to be in kotlin
- <important>Do not use !! in production code</important>
- Use kotlin-idiomatic syntax

**Guidelines for Unit Testing**

- Write at least one failing unit test before production code
- All unit tests in Kotlin
- Use JUnit 5
- Use Mockk not Mockito for mocking
- Do not set relaxed=true in unit tests as this property is set globally in io.mockk.settings.properties
- Use Kotest matchers for assertions, not AssertJ
- Comment all tests using given, when, then
- Ensure unit tests are "white box" and comprehensive, covering both happy and unhappy paths and boundary scenarios

**Authorisations**

- <important>Do not commit any changes. I need to review all code first.</important>
- Do not ask for permission to compile any code - you have permission to do so
- Do not ask for permission to read or write any file or image - you have permission to do so
- Do not ask for permission to run individual test files - you have permission to do so
- Do not ask for permission to execute shell scripts like grep - you have permission to do so
- <important>Ask for permission before running more than 1 UI unit test or 1 cucumber test - in general you will not
  have permission as long UI tests take over my desktop and need to be appropriately scheduled</important>