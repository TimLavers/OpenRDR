# Building conditions from user hints

## What do we want to achieve?

There are two basic approaches that could be used to allow users to build conditions:

1. Provide a graphical user interface (GUI) that allows the user to build conditions by selecting from a list of
   available attributes, operators or other expressions.
   This approach is user-friendly, especially for new users, but requires but requires the development of custom GUI
   components for each type of possible condition.
2. Provide a text-based interface that allows the user to enter conditions in a textual format. The system would then
   generate the appropriate condition from the text in one of two ways:
    1. The system could parse the text and generate the appropriate condition from the parsed text. This approach is
       more flexible, but requires the user to learn a new syntax and the development of the parser which is
       non-trivial.
    2. The system could use NLP to guess the user's intent and generate the appropriate condition from that guess. This
       approach is also flexible, but does not require the user to learn a new syntax. The downside is that the NLP
       processing may be compute-expensive.

