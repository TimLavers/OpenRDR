Feature: A sample KB can be created that has the Contact Lense Prescription cases.

#  We check the data for each case and check that the interpretations of a couple of cases are blank.
@single
  Scenario: Contact Lenses cases
    Given I start the client application
    And I create a Knowledge Base with the name ContactLenseCases based on the "Contact Lense Prescription - cases only" sample
    And pause for 2 seconds
    Then the count of the number of cases is 24
    And pause for 1 second
    When I select case Case1
    Then I see these case values:
      | age               | young          | |
      | prescription      | myope          | |
      | astigmatism       | not_astigmatic | |
      | tear production   | reduced        | |

    When I select case Case2
    Then I see these case values:
      | age               | young          | |
      | prescription      | myope          | |
      | astigmatism       | not_astigmatic | |
      | tear production   | normal         | |
    And the interpretation field should be empty

    When I select case Case3
    Then I see these case values:
      | age               | young      | |
      | prescription      | myope      | |
      | astigmatism       | astigmatic | |
      | tear production   | reduced    | |
    And the interpretation field should be empty

    When I select case Case4
    Then I see these case values:
      | age               | young      | |
      | prescription      | myope      | |
      | astigmatism       | astigmatic | |
      | tear production   | normal     | |

    When I select case Case5
    Then I see these case values:
      | age               | young          | |
      | prescription      | hypermetrope   | |
      | astigmatism       | not_astigmatic | |
      | tear production   | reduced        | |

    When I select case Case6
    Then I see these case values:
      | age               | young          | |
      | prescription      | hypermetrope   | |
      | astigmatism       | not_astigmatic | |
      | tear production   | normal         | |

    When I select case Case7
    Then I see these case values:
      | age               | young        | |
      | prescription      | hypermetrope | |
      | astigmatism       | astigmatic   | |
      | tear production   | reduced      | |

    When I select case Case8
    Then I see these case values:
      | age               | young         | |
      | prescription      | hypermetrope  | |
      | astigmatism       | astigmatic    | |
      | tear production   | normal        | |

    When I select case Case9
    Then I see these case values:
      | age               | pre_presbyopic | |
      | prescription      | myope          | |
      | astigmatism       | not_astigmatic | |
      | tear production   | reduced        | |

    When I select case Case10
    Then I see these case values:
      | age               | pre_presbyopic | |
      | prescription      | myope          | |
      | astigmatism       | not_astigmatic | |
      | tear production   | normal         | |

    When I select case Case11
    Then I see these case values:
      | age               | pre_presbyopic | |
      | prescription      | myope          | |
      | astigmatism       | astigmatic     | |
      | tear production   | reduced        | |

    When I select case Case12
    Then I see these case values:
      | age               | pre_presbyopic | |
      | prescription      | myope          | |
      | astigmatism       | astigmatic     | |
      | tear production   | normal         | |

    When I select case Case13
    Then I see these case values:
      | age               | pre_presbyopic | |
      | prescription      | hypermetrope   | |
      | astigmatism       | not_astigmatic | |
      | tear production   | reduced        | |

    When I select case Case14
    Then I see these case values:
      | age               | pre_presbyopic | |
      | prescription      | hypermetrope   | |
      | astigmatism       | not_astigmatic | |
      | tear production   | normal         | |

    When I select case Case15
    Then I see these case values:
      | age               | pre_presbyopic | |
      | prescription      | hypermetrope   | |
      | astigmatism       | astigmatic     | |
      | tear production   | reduced        | |

    When I select case Case16
    Then I see these case values:
      | age               | pre_presbyopic | |
      | prescription      | hypermetrope   | |
      | astigmatism       | astigmatic     | |
      | tear production   | normal         | |

    When I select case Case17
    Then I see these case values:
      | age               | presbyopic     | |
      | prescription      | myope          | |
      | astigmatism       | not_astigmatic | |
      | tear production   | reduced        | |

    When I select case Case18
    Then I see these case values:
      | age               | presbyopic     | |
      | prescription      | myope          | |
      | astigmatism       | not_astigmatic | |
      | tear production   | normal         | |

    When I select case Case19
    Then I see these case values:
      | age               | presbyopic | |
      | prescription      | myope      | |
      | astigmatism       | astigmatic | |
      | tear production   | reduced    | |

    When I select case Case20
    Then I see these case values:
      | age               | presbyopic | |
      | prescription      | myope      | |
      | astigmatism       | astigmatic | |
      | tear production   | normal     | |

    When I select case Case21
    Then I see these case values:
      | age               | presbyopic     | |
      | prescription      | hypermetrope   | |
      | astigmatism       | not_astigmatic | |
      | tear production   | reduced        | |

    When I select case Case22
    Then I see these case values:
      | age               | presbyopic     | |
      | prescription      | hypermetrope   | |
      | astigmatism       | not_astigmatic | |
      | tear production   | normal         | |

    When I select case Case23
    Then I see these case values:
      | age               | presbyopic  | |
      | prescription      | hypermetrope | |
      | astigmatism       | astigmatic   | |
      | tear production   | reduced      | |

    When I select case Case24
    Then I see these case values:
      | age               | presbyopic   | |
      | prescription      | hypermetrope | |
      | astigmatism       | astigmatic   | |
      | tear production   | normal       | |

    And stop the client application
