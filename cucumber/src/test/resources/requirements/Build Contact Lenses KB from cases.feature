Feature: A sample KB can be created that has the Contact Lense Prescription cases.

  @single
  Scenario: Build Contact Lenses KB from cases
    Given I start the client application
    And I create a Knowledge Base with the name ContactLenses based on the "Contact Lense Prescription - cases only" sample
    And pause for 2 seconds
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
    And pause for 2 seconds
    And I select the interpretation tab
    And pause
    And I build a rule to remove the comment "soft" with conditions
      | age is "presbyopic"     |
      | prescription is "myope" |

    And I select case Case24
    And I select the interpretation tab
    And I build a rule to remove the comment "hard" with conditions
      | age is "presbyopic"            |
      | prescription is "hypermetrope" |

    And stop the client application
