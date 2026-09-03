Feature: Cases can be searched by condition

  Scenario: The search cases list is initially empty
    Given I start the client application
    Then I should see no cases in the search case list

    @single
  Scenario: If a search by condition finds no cases then the search case list remains empty
    Given there are 2 cases stored on the server having data:
      | Sun   | hot |
      | Waves | 1.5 |
    And I start the client application
    And I see the case Case1 as the current case
    And I should see no cases in the search case list
    And I search by condition "Waves > 3.0"
    Then I should see a chat message saying that no cases were found
    And I should see no cases in the search case list

  Scenario: The number of results returned by a search for cases by condition can be limited
    Given there are 25 cases stored on the server having data:
      | Sun   | hot |
      | Waves | 1.5 |
    And I start the client application
    And I see the case Case1 as the current case
    And I should see no cases in the search case list
    And I search for at most 10 cases satisfying the condition "waves > 1"
    Then I should see a chat message saying that 10 cases were found
    And the search case list should have 10 cases

  Scenario: The cornerstone cases can be searched by condition
    Given there are 10 cornerstone cases stored on the server having data:
      | Wind  | high |
      | Waves | 3.5  |
    And I start the client application
    And I search for at most 2 cornerstone cases satisfying the condition "wind is high"
    Then I should see a chat message saying that 2 cases were found
    And the search case list should have 2 cases

  Scenario: Case search amongst processed cases does not find cornerstone cases
    Given there are 2 cases stored on the server having data:
      | Wind  | low |
      | Waves | 0.5 |
    And there are 2 cornerstone cases stored on the server having data:
      | Wind  | high |
      | Waves | 3.5  |
    And I start the client application
    And I see the case Case1 as the current case
    And I should see no cases in the search case list
    And I search by condition "Waves > 3.0"
    Then I should see a chat message saying that no cases were found
    And I should see no cases in the search case list

  Scenario: Case search amongst cornerstone cases does not find processed cases
    Given there are 2 cases stored on the server having data:
      | Wind  | low |
      | Waves | 0.5 |
    And there are 2 cornerstone cases stored on the server having data:
      | Wind  | high |
      | Waves | 3.5  |
    And I start the client application
    And I see the case Case1 as the current case
    And I should see no cases in the search case list
    And I search for at most 2 cornerstone cases satisfying the condition "wind is low"
    Then I should see a chat message saying that no cases were found
    And I should see no cases in the search case list

  Scenario: Successive searches append to the search case list
    Given there are 2 cases stored on the server having data:
      | Wind  | low |
      | Waves | 0.5 |
    And there are 2 cornerstone cases stored on the server having data:
      | Wind  | high |
      | Waves | 3.5  |
    And I start the client application
    And I see the case Case1 as the current case
    And I should see no cases in the search case list
    And I search for at most 2 cornerstone cases satisfying the condition "wind is high"
    Then I should see a chat message saying that 2 cases were found
    And the search case list should have 2 cases
    And I search for at most 20 cases satisfying the condition "wind is low"
    Then I should see a chat message saying that 2 cases were found
    And the search case list should have 4 cases

  Scenario: The search case list can be cleared at the start of a search
    Given there are 2 cases stored on the server having data:
      | Wind  | low |
      | Waves | 0.5 |
    And there are 2 cornerstone cases stored on the server having data:
      | Wind  | high |
      | Waves | 3.5  |
    And I start the client application
    And I see the case Case1 as the current case
    And I search for at most 2 cornerstone cases satisfying the condition "wind is high"
    Then I should see a chat message saying that 2 cases were found
    And the search case list should have 2 cases
    And I search for at most 20 cases satisfying the condition "wind is low" and clearing the search list first
    Then I should see a chat message saying that 2 cases were found
    And the search case list should have 2 cases

