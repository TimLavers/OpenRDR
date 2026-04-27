Feature: The user should be able to see all test dates and results for a case

  Scenario: Case with two episodes
    Given the configured case Case4 is stored on the server
    And I start the client application
    When I see the case Case4 as the current case
    Then I should see these episode dates:
      | 2022-08-05 12:31 |
      | 2022-08-06 02:25 |
    And I should see these attributes:
      | TSH |
      | Stuff |
    And I should see these values for 'TSH':
      | 0.67 |
      | 2.75 |
    And I should see '0.50 - 4.0' as reference range for 'TSH'
    And I should see these values for 'Stuff':
      | 12.4 |
      | 6.7  |
    And I should see '' as reference range for 'Stuff'
