Feature: Rose experiment.

  Scenario: Provide cases
    Given I start the client application
    Then the chatbot response contains the following terms:
      | no knowledge bases yet | create |
    And no knowledge base is shown as selected
    When I enter the following text into the chat panel:
      | yes |
    Then the chatbot response contains the following terms:
      | What would you like to call it? |
    When I enter the following text into the chat panel:
      | Rose |
    Then the chatbot response contains the following terms:
      | Created | Rose |
    And the displayed KB name is now Rose
    And I send a case to "Rose" for each row in the rose cases file
    And the backdoor selects the Knowledge Base "Rose"
#    And the count of the number of cases is 12
    When backdoor rules are built as follows:
      | Case               | Add          | Remove | Conditions                                     |
      | ALK p.Lys1525del 1 | activating mutation, possible indication for ALK inhibitors.  | | canonical contains "cervical cancer"           |

    And pause for 60 seconds

#  ALK (p.Lys1525del) activating mutation, possible indication for ALK inhibitors.
#  activating mutation, possible indication for ALK inhibitors.