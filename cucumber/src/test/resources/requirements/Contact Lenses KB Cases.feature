Feature: The cases described in the Contact Lenses sample KB can be represented in OpenRDR

  @single
  Scenario: Contact Lenses cases
    Given the Contact Lenses sample KB has been loaded
    And I start the client application
    Then the count of the number of cases is 24
    And pause for 10 seconds
    When I select case Case1
    Then I see these case values:
      | age               | young           | |
      | prescription      | myope           | |
      | astigmatism       | not_astigmatic  | |
      | tear production   | reduced         | |

    When I select case Case2
    Then I see these case values:
      | age               | young           | |
      | prescription      | myope           | |
      | astigmatism       | not_astigmatic  | |
      | tear production   | normal          | |

    And stop the client application
