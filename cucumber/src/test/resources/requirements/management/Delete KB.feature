Feature: Knowledge Base deletion

  @ignore
  @no_default_kb
  Scenario: A Knowledge Base can be deleted
    Given A Knowledge Base called Thyroids has been created
    Given A Knowledge Base called Glucose has been created
    Given A Knowledge Base called Lipids has been created
    And I start the client application
    And I activate the KB management control
    Then pause for 5 seconds
    Then I should see this list of available KBs:
      | Glucose  |
      | Lipids   |
      | Thyroids |
    And I activate the KB management control

    And stop the client application
