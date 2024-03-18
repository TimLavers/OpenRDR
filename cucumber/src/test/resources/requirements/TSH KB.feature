Feature: The cases described in the TSH paper can be represented in OpenRDR

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

    And stop the client application
