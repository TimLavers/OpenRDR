# Conclusion Requirements
`Conclusion`s are textual comments added to interpretations by rules.


| Requirement            | Description                                                          | Validation |
|------------------------|----------------------------------------------------------------------|------------|
| `Conclusion` ids       | Each `Conclusion` has an id, which is an integer.                    | Conc-1     |
| Text not blank         | The text of a `Conclusion` cannot be blank.                          | Conc-2     |
| Maximum length of text | There can be at most 1,024 characters in the text of a `Conclusion`. | Conc-3     |

Note that in some later version of the software, it will be possible to edit `Conclusion`s, 
which will open the possibility of a `Conclusion` being edited to have the same text as another.
So, unlike `Attribute`s, it will be possible to have two or more `Conclusion`s with the same text.