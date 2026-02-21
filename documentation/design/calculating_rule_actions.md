# Calculating the rule actions from changes made to the interpretive report

## What is a rule action?

A rule comprises of an action and a set of boolean conditions.  
The conditions are evaluated against the data in the case, and if all are true, the action of the rule is applied.
The action defines the change made to the interpretation by that rule

In this example domain, the possible rule actions are limited to:

- Adding a sentence
- Removing a sentence
- Replacing one sentence with another sentence

The list of all sentences given by the rules in the knowledge base forms the interpretive report for each case.

## Options for determining the rule action

### Option 1: Ask the user to provide the rule action
Using the chat, ask the user to describe the change that they want to make to the interpretive report, namely, adding, removing or replacing a sentence.

This is the simplest option, but it requires the user to identify a sentence to remove or replace. If adding a sentence, the user will need to provide the text of the sentence,
and there needs to be the option of adding a comment that has already been used for another rule.

### Option 2: Use a text difference algorithm

In this approach, we could allow the user to make arbitrary changes to the report, then use a text difference algorithm to determine the rule actions from the changes made to the interpretive report.
They would then add a rule for each change, or possibly a single rule for all changes.

A suggested text difference algorithm is the [The Myers difference algorithm.](https://www.nathaniel.ai/myers-diff/#:~:text=In%201986%2C%20Eugene%20Myers%20published,transforming%20one%20sequence%20into%20another.)
where the sentences are the atomic units of comparison, rather than words or characters.

The steps are as follows:

1. Allow the user to edit the interpretive report. Once completed, this becomes the "approved" report.
2. Split the approved report into sentences using the period + space characters as the delimiter.
2. The interpretive report is already split into sentences.
3. Map each sentence in the approved report to an alphabetic character, e.g. A, B, C, etc, so that the approved report
   is represented as a list of single-digit characters. (We assume in this domain that there are at most 26 unique
   sentences in any report. The algorithm can be easily extended if this assumption is not true for some domain.)
4. Do the same for the sentences in the interpretive report.
5. Use the Myers algorithm to determine the differences between the two lists of characters. The implementation in this
   project is based on
   the [Java diff Utilities](https://mvnrepository.com/artifact/io.github.java-diff-utils/java-diff-utils)
6. The algorithm returns a list of rows, with each row containing 1, 2 or 4 characters as follows:
   - A row with a single character, e.g. "A" means that the sentence corresponding to A in the original report was not
     changed.
   - A row with two characters is of the form +A or -A which means that the sentence corresponding to A was added or
     removed respectively.
   - A row with four characters is of the form +A-B which means that the sentence corresponding to A was replaced by the
     sentence corresponding to B.

## The recommended approach
Option 1 is the recommended (and implemented)approach as it is the simplest and most transparent to the user. There is the disadvantage that 
each change can only be made to one sentence at a time. The cannot, for example, replace two consecutive sentences with a single sentence. 

Option 2 was trialled, but it was found to be more complex for the user to use, and the difference algorithm above was not always able to determine the correct changes.

This may be alleviated by using a more advanced difference algorithm, or by getting an LLM to determine the rule actions from the changes made to the interpretive report.




