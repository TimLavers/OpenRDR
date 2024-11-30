# Building rule conditions

## What do we want to achieve?

There are several basic approaches that could be used to allow users to build conditions for the rules they define:

1. Provide a graphical user interface (GUI) that would allow the user to build conditions by selecting from a list of
   available attributes, operators or other expressions.
   This approach is user-friendly, especially for new or occasional users, but would require the development of custom
   GUI
   components for each type of possible condition. This approach would work well for simple conditions, e.g. to
   represent the fact that an attribute value is above some constant value. However, to represent more complex
   conditions, e.g. that over the past year an attribute had a high value at least three times, then a much more complex
   GUI would be required.
2. Define a formal condition syntax, that is, textual format for each type of condition, e.g. `Glucose is high` or
   `at least 3 Glucose are high in the past year`, and then develop a parser to translate what the user has entered into
   the corresponding condition. This approach is flexible, but requires the user to learn and remember this syntax.
   Furthermore, the development of the parser would be non-trivial. Internationalization of the syntax would also be a
   challenge for the parser.
3. Use the translation capability of an LLM to parse the user's expression directly into a condition without requiring
   the user to enter a specified syntax. So rather than having to enter a formal expression like `Glucose is high`, the
   user could enter a variety of expressions that convey this concept, e.g. `Elevated Glucose` or `Raised Glucose`. As
   well as being even more flexible, this approach would allow the user to represent conditions using their own
   terminology. Internationalization would come for "free" as general-purpose LLMs are now trained on all popular
   languages. The downside is that there may be a cost (financial and environmental) for use of the LLM.

We have chosen the third approach because:

- it is the most flexible and makes the least demands on the user,
- it avoids the need to develop a custom GUI or parser,
- it provides internationalization with no additional software effort.


