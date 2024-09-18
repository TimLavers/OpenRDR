Feature: Reviewing the interpretation of a case

  Scenario: The user should be able to see the interpretation of a case
    Given case Bondi is provided having data:
      | Wave | excellent |
    And the interpretation of the case Bondi is "Go to Bondi."
    When I start the client application
    Then I should see the case Bondi as the current case
    And the interpretation should contain the text "Go to Bondi."

  Scenario: The user should be able to see different interpretations for different cases
    Given case Bondi is provided having data:
      | Wave | excellent |
    And case Manly is provided having data:
      | Swimming | excellent |
    And the interpretation of the case Bondi includes "Go to Bondi." because of condition "Wave is in case"
    And the interpretation of the case Manly includes "Go to Manly." because of condition "Swimming is in case"
    And I start the client application
    And I should see the case Bondi as the current case
    And the interpretation should contain the text "Go to Bondi."
    When I select the case Manly
    Then the interpretation should contain the text "Go to Manly."

  Scenario: The user should be able to view the conditions giving a comment
    And case Bondi is provided having data:
      | Waves | excellent |
      | Sun   | shining   |
    And a rule exists to add the comment "Go to Bondi." to case Bondi for the following conditions:
      | Waves is in case |
    And a rule exists to add the comment "Bring sunscreen." to case Bondi for the following conditions:
      | Sun is in case |
    And I start the client application
    And I should see the case Bondi as the current case
    And the interpretation should contain the text "Go to Bondi. Bring sunscreen."
    Then the condition showing for the comment "Go to Bondi" is:
      | Waves is in case |
    Then the condition showing for the comment "Bring sunscreen." is:
      | Sun is in case |

  Scenario: No conditions should be shown for a comment that is given with no conditions
    And case Bondi is provided having data:
      | Waves | excellent |
      | Sun   | shining   |
    And a rule exists to add the comment "Go to Bondi." to case Bondi with no conditions
    When I start the client application
    And I should see the case Bondi as the current case
    And the interpretation should contain the text "Go to Bondi."
    And move pointer to the comment "Go to Bondi."
    And pause for 1 second
    And the interpretation should be "Go to Bondi."
    Then no conditions should be showing for the comment "Go to Bondi."

  Scenario: No conditions should be shown after building a rule with no conditions to give a comment
    And case Bondi is provided having data:
      | Waves | excellent |
      | Sun   | shining   |
    And I start the client application
    And I should see the case Bondi as the current case
    When I build a rule to add the comment "Go to Bondi."
    And move pointer to the comment "Go to Bondi."
    And pause for 1 second
    And the interpretation should be "Go to Bondi."
    Then no conditions should be showing for the comment "Go to Bondi."

