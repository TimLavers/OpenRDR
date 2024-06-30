Feature: A sample KB can be created that has the Contact Lense Prescription cases.

  @single
  Scenario: Build Contact Lenses KB from cases
    Given I start the client application
    And I create a Knowledge Base with the name ContactLenses based on the "Contact Lense Prescription - cases only" sample
    And pause for 2 seconds
    Then the count of the number of cases is 24
    And I select case Case2
    And I enter the text "soft" in the interpretation field
    And pause



    And stop the client application
