@ignore
Feature: The Contact Lense Prescription KB can be built with the user interface.
  Scenario: Build the Contact Lenses KB from cases
    Given I start the client application
    And I create a Knowledge Base with the name ContactLenses based on the "Contact Lense Prescription - cases only" sample
    Then the count of the number of cases is 24
    And I select case Case2
    And I build a rule to add the comment "soft" with conditions
      | astigmatism is "not_astigmatic" |
      | tear production is "normal"     |

    And I select case Case4
    And I build a rule to add the comment "hard" with conditions
      | astigmatism is "astigmatic" |
      | tear production is "normal" |

    And I select case Case16
    And I select the interpretation tab
    And I build a rule to remove the comment "hard" with conditions
      | age is "pre_presbyopic"        |
      | prescription is "hypermetrope" |

    And I select case Case18
    And I select the interpretation tab
    And I build a rule to remove the comment "soft" with conditions
      | age is "presbyopic"     |
      | prescription is "myope" |

    And I select case Case24
    And I select the interpretation tab
    And I build a rule to remove the comment "hard" with conditions
      | age is "presbyopic"            |
      | prescription is "hypermetrope" |

    Then the cases should have interpretations as follows:
      | Case1  |      |
      | Case2  | soft |
      | Case3  |      |
      | Case4  | hard |
      | Case5  |      |
      | Case6  | soft |
      | Case7  |      |
      | Case8  | hard |
      | Case9  |      |
      | Case10 | soft |
      | Case11 |      |
      | Case12 | hard |
      | Case13 |      |
      | Case14 | soft |
      | Case15 |      |
      | Case16 |      |
      | Case17 |      |
      | Case18 |      |
      | Case19 |      |
      | Case20 | hard |
      | Case21 |      |
      | Case22 | soft |
      | Case23 |      |
      | Case24 |      |
    And stop the client application
