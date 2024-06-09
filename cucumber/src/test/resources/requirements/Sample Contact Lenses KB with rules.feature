Feature: A sample KB can be created that has the Contact Lense Prescription cases and rules.

#  We just check a couple of the cases as there's another test that checks them all./
  Scenario: Contact Lenses cases
    Given I start the client application
    And I create a Knowledge Base with the name ContactLenseRules based on the "Contact Lense Prescription" sample
    Then the count of the number of cases is 24
    And pause for 2 seconds

    When I select case Case1
    Then I see these case values:
      | age               | young          | |
      | prescription      | myope          | |
      | astigmatism       | not_astigmatic | |
      | tear production   | reduced        | |
    And the interpretation field should be empty

    When I select case Case2
    Then I see these case values:
      | age               | young          | |
      | prescription      | myope          | |
      | astigmatism       | not_astigmatic | |
      | tear production   | normal         | |
    And the interpretation should be "soft"

    When I select case Case3
    Then the interpretation field should be empty

    When I select case Case4
    Then the interpretation should be "hard"

    When I select case Case5
    Then the interpretation field should be empty

    When I select case Case6
    Then the interpretation should be "soft"

    When I select case Case7
    Then the interpretation field should be empty

    When I select case Case8
    Then the interpretation should be "hard"

    When I select case Case9
    Then the interpretation field should be empty

    When I select case Case10
    Then the interpretation should be "soft"

    When I select case Case11
    Then the interpretation field should be empty

    When I select case Case12
    Then the interpretation should be "hard"

    When I select case Case13
    Then the interpretation field should be empty

    When I select case Case14
    Then the interpretation should be "soft"

    When I select case Case15
    Then the interpretation field should be empty

    When I select case Case16
    And pause for 2 seconds
    Then the interpretation field should be empty

    When I select case Case17
    Then the interpretation field should be empty

    When I select case Case18
    Then the interpretation field should be empty

    When I select case Case19
    Then the interpretation field should be empty

    When I select case Case20
    Then the interpretation should be "hard"

    When I select case Case21
    Then the interpretation field should be empty

    When I select case Case22
    Then the interpretation should be "soft"

    When I select case Case23
    Then the interpretation field should be empty

    When I select case Case24
    Then the interpretation field should be empty

    And stop the client application
