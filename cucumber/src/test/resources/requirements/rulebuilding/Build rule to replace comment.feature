Feature: The user can make a rule that replaces a comment the interpretive report

  Scenario: The user should be able to build a rule to replace a comment
    Given a list of cases with the following names is stored on the server:
      | Case1 |
      | Case2 |
    And the interpretation of the case Case1 is "Go to Bondi."
    And I start the client application
    And I should see the case Case1 as the current case
    When I build a rule to replace that comment by "Go to Maroubra."
    Then the interpretation should be "Go to Maroubra."
    And select the case Case2
    And  the interpretation should be "Go to Maroubra."

  Scenario: The user should be able to build a rule to replace a comment with a condition they have selected
    Given case Manly is provided having data:
      | Sun      | hot      |
      | Swimming | pleasant |
    And the interpretation of the case Manly is "Go to Bondi."
    And I start the client application
    When I build a rule to replace the comment "Go to Bondi." by "Go to Manly." with the condition
      | "Sun is in case" |
    Then  the interpretation should contain the text "Go to Manly."
    And case Bondi is provided having data:
      | Wave | poor |
    And I select case Bondi
    And  the interpretation report should be is empty

  Scenario: The conditions shown for a comment that is a replacement should include the conditions for the comment that has been replaced
    Given case Beach is provided having data:
      | Sun  | too hot |
      | Wave | poor    |
    And I start the client application
    And I build a rule to add the comment "Go to Bondi." with condition
      | "Wave is in case" |
    And  the interpretation should be "Go to Bondi."
    When I request another change to be made to the report
    And I build a rule to replace the comment "Go to Bondi." by "Go to Manly." with the condition
      | "Sun is in case" |
    Then the interpretation should be "Go to Manly."
#    Doesn't work on TL's computer, TODO fix
    And the conditions showing for the comment "Go to Manly." are:
      | Wave is in case |
      | Sun is in case  |

  Scenario: A comment given for the case must be identified before the user can start a rule to replace it
    Given a case with name Case1 is stored on the server
    And the interpretation of the case Case1 is "Go to Bondi."
    And I start the client application
    And I should see the case Case1 as the current case
    When I try to build a rule to replace the non-existing comment "Go to Maroubra"
    Then the model should respond with a message containing:
      | Go to Bondi |
