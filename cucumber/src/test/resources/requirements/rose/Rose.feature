Feature: Rose experiment.

  @single
  Scenario: Provide cases
    Given I start the client application
    And I create a Knowledge Base with the name Rose
    And I send a case to "Rose" for each row in the rose cases file
    And the backdoor selects the Knowledge Base "Rose"
#    And the count of the number of cases is 12
    And pause for 30 seconds
