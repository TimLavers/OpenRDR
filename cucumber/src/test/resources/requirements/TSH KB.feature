Feature: The cases described in the TSH paper can be represented in OpenRDR

  @single
  @tsh
  Scenario: TSH cases
    Given the TSH sample KB has been loaded
    And I start the client application
    Then the count of the number of cases is 34

    When I select case 1.4.1
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 28                |            |
      | TSH               | 0.67 mU/L         | 0.50 - 4.0 |
      | Free T4           | 16 pmol/L         |   10 - 20  |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Lethargy.         |            |

    When I select case 1.4.2
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 28                |            |
      | TSH               | 0.67 mU/L         | 0.50 - 4.0 |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Lethargy.         |            |

    When I select case 1.4.3
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 36                |            |
      | TSH               | 0.74 mU/L         | 0.50 - 4.0 |
      | Free T4           | 8 pmol/L          |   10 - 20  |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Weight loss.      |            |

    When I select case 1.4.4
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 57                |            |
      | TSH               | 7.3 mU/L          | 0.50 - 4.0 |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Weight gain.      |            |

    When I select case 1.4.5
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 57                |            |
      | TSH               | 7.3 mU/L          | 0.50 - 4.0 |
      | Free T4           | 13 pmol/L         |   10 - 20  |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Weight gain.      |            |

    When I select case 1.4.6
    Then I see these case values:
      | Sex               | M                 |            |
      | Age               | 76                |            |
      | TSH               | 4.5 mU/L          | 0.50 - 4.0 |
      | Free T4           | 15 pmol/L         |   10 - 20  |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Routine check.    |            |

    When I select case 1.4.7
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 62                |            |
      | TSH               | 14.0 mU/L         | 0.50 - 4.0 |
      | Free T4           | 13 pmol/L         |   10 - 20  |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Constipation.     |            |

    When I select case 1.4.8
    Then I see these case values:
      | Sex               | F                                 |            |
      | Age               | 27                                |            |
      | TSH               | 0.05 mU/L                         | 0.50 - 4.0 |
      | Free T4           | 13 pmol/L                         |   10 - 20  |
      | Patient Location  | Obstetric clinic.                 |            |
      | Tests             | TFTs                              |            |
      | Clinical Notes    | Period of amenorrhea 12/40 weeks. |            |

    When I select case 1.4.9
    Then I see these case values:
      | Sex               | F                 |            |
      | Age               | 32                |            |
      | TSH               | 4.6 mU/L          | 0.50 - 4.0 |
      | Free T4           | 13 pmol/L         |   10 - 20  |
      | TPO Antibodies    | 33 kU/L           |   < 6      |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Trying for a baby.|            |

    When I select case 1.4.10
    Then I see these case values:
      | Sex               | M                  |            |
      | Age               | 55                 |            |
      | TSH               | 0.02 mU/L          | 0.50 - 4.0 |
      | Free T4           | 18 pmol/L          |   10 - 20  |
      | Patient Location  | General Practice.  |            |
      | Tests             | TFTs               |            |
      | Clinical Notes    | Feeling very tired.|            |

    When I select case 1.4.11
    Then I see these case values:
      | Sex               | M                 |            |
      | Age               | 55                |            |
      | TSH               | 0.02 mU/L         | 0.50 - 4.0 |
      | Free T4           | 18 pmol/L         |   10 - 20  |
      | Free T3           | 6.1 pmol/L        | 3.0 - 5.5  |
      | Patient Location  | General Practice. |            |
      | Tests             | TFTs              |            |
      | Clinical Notes    | Hyperthyroid?     |            |

    When I select case 1.4.12
    Then I see these case values:
      | Sex               | M                  |            |
      | Age               | 74                 |            |
      | TSH               | 59 mU/L            | 0.50 - 4.0 |
      | Free T4           | <5 pmol/L          |   10 - 20  |
      | Patient Location  | General Practice.  |            |
      | Tests             | TFTs               |            |
      | Clinical Notes    | Hypothyroid?       |            |

    And stop the client application
