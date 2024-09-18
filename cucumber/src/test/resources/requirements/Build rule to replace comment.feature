Feature: The user can make a rule that replaces a comment the interpretive report

  Scenario: The user should be able to build a rule to replace a comment
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And the interpretation of the case Case1 is "Go to Bondi."
    And I start the client application
    And I should see the case Case1 as the current case
    And  the interpretation should contain the text "Go to Bondi."
    When I build a rule to replace the comment "Go to Bondi." by "Go to Maroubra."
    Then the interpretation should be "Go to Maroubra."
    And select the case Case2
    And  the interpretation should be "Go to Maroubra."
    And stop the client application

  Scenario: The user should be able to build a rule to replace a comment with a condition they have selected
    Given case Bondi is provided having data:
      | Wave | poor |
    And case Manly is provided having data:
      | Sun      | hot      |
      | Swimming | pleasant |
    And the interpretation of the case Bondi is "Go to Bondi."
    And I start the client application
    And I select case Manly
    And  the interpretation should contain the text "Go to Bondi."
    When I build a rule to replace the comment "Go to Bondi." by "Go to Manly." with the condition "Sun is in case"
    Then  the interpretation should contain the text "Go to Manly."
    And I select case Bondi
    And  the interpretation should contain the text "Go to Bondi."
    And stop the client application

  Scenario: The conditions shown for a comment that is a replacement should include the conditions for the comment that has been replaced
    Given case Beach is provided having data:
      | Sun  | too hot |
      | Wave | poor    |
    And I start the client application
    And I build a rule to add the comment "Go to Bondi." with the condition "Wave is in case"
    And  the interpretation should be "Go to Bondi."
    When I build a rule to replace the comment "Go to Bondi." by "Go to Manly." with the condition "Sun is in case"
    Then the interpretation should be "Go to Manly."
    And the conditions showing for the comment "Go to Manly." are:
      | Wave is in case |
      | Sun is in case  |
    And stop the client application

  Scenario: A comment given for the case must be selected before the user can start a rule to replace it
    Given a case with name Case1 is stored on the server
    And the interpretation of the case Case1 is "Go to Bondi."
    And I start the client application
    And I should see the case Case1 as the current case
    And I start to build a rule to replace a comment
    When I enter "Maroubra" as the filter to select a comment to replace
    Then the OK button to start the rule to replace the comment should be disabled
    And stop the client application