# Condition Parsing Using a Large Language Model

## What do we want to achieve?

When the user is building a rule, to unambiguously translate their user-entered expression to the corresponding
Condition instance. We want to avoid the need for the user to learn a formal syntax for expressing conditions. We want
to allow the user to use their own terminology to express conditions. We want to allow the user to express conditions in
any language that the LLM has been trained on.

For example, all the following expressions should be translated into an EpisodicCondition with attribute `Glucose` and
TestResultPredicate `High`

- `Glucose is high`
- `Glucose is elevated`
- `Elevated Glucose`
- `Raised Glucose`
- `Glucose is above normal`

## How do we achieve this?

The `hints` subproject provides the functionality to translate a user expression into a Condition instance using a
Gemini LLM. The process is as follows:

1. When building a rule, the user enters an expression describing some condition, e.g. `Glucose is no less than 5.5`.
2. The attribute in the expression is identified and replaced by a placeholder 'x'. The expression now becomes
   `x is no less than 5.5`. This is done to make it easier for the LLM to identify the significant parts of the
   expression, which in this example are "is no less than" and "5.5".
3. Gemini is given a structured prompt to convert the expression into a list of tokens, the first token being the name
   of a function that will construct the condition, and the subsequent tokens identifying any parameters needed by that
   function.
4. The structured prompt contains examples for the types of translations required. Here is the prompt for the above
   expression:

```
Your task is to identify the components in an expression.

Output the single component, or if several components, separate them by a comma.
Examples of expressions with the expected output are:

Input: x is high
Output: High
Input: x is elevated
Output: High
Input: x is less than or equal to y
Output: LessThanOrEqualTo, y
(etc.)

Here is the expression: x no less than 5.5
Generate output without additional string.
```

where some of the Input: and Output: examples in the prompt have been omitted for brevity. The full list is in the file
`hints/src/main/resources/training_set.txt`.

5. In this example, Gemini provides the output `GreaterThanOrEqualTo, 5.5` Note that it has cleverly inferred the
   correct function name `GreaterThanOrEqualTo` even though that term was not provided as an example in the prompt!
6. The class `ConditionConstructors` provides the functions to construct the conditions. These functions are called
   reflexively from the tokens provided by Gemini. For this example, the following function is called:

```
fun GreaterThanOrEqualTo(attribute: Attribute, userExpression: String, d: String) = EpisodicCondition(...)

```  

where
attribute = `Glucose`\
userExpression = `Glucose is no less than 5.5`\
d = `5.5`

The user expression is stored with the condition instance so that it can be displayed to the user when they are viewing
the reasons (i.e. conditions) why a particular comment is given or not given for a case.

7. The condition generated from the LLM is then evaluated for the current case and is only suggested to the user if it
   evaluates as true. The formal syntax for the condition (`Glucose >= 5.5` for this example) is also shown as a
   tool-tip to allow the user to confirm that their expression has been correctly translated.



