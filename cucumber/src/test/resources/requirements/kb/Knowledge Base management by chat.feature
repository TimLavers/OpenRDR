Feature: Managing knowledge bases through the chat

  Background:
    Given there is a knowledge base called Thyroids

  Scenario: The available knowledge bases can be listed
    Given A Knowledge Base called B has been created
    And A Knowledge Base called C has been created
    And A Knowledge Base called A has been created
    And I start the client application
    When I enter the following text into the chat panel:
      | What knowledge bases are available? |
    Then the chatbot response consists of the following lines:
      | A (open) |
      | B        |
      | C        |
      | Thyroids |

  Scenario: A knowledge base can be opened by name
    Given A Knowledge Base called A has been created
    And case CaseA1 for KB A is provided having data:
      | Sun | hot |
    And A Knowledge Base called B has been created
    And case CaseB1 for KB B is provided having data:
      | Sun | cold |
    And I start the client application
    And the displayed KB name is A
    When I enter the following text into the chat panel:
      | Please open B |
    Then the chatbot response contains the following terms:
      | Opened | B |
    And the displayed KB name is now B
    And I should see the case CaseB1 as the current case
    When I enter the following text into the chat panel:
      | Please open a |
    Then the displayed KB name is now A
    And I should see the case CaseA1 as the current case

  Scenario: Opening an unknown knowledge base lists the ones that exist
    Given A Knowledge Base called Glucose has been created
    And I start the client application
    When I enter the following text into the chat panel:
      | Open Lipids |
    Then the chatbot response contains the following terms:
      | no knowledge base named | Lipids | Glucose | Thyroids |
    And the displayed KB name is Glucose

  Scenario: Opening a knowledge base by part of its name asks first
    Given A Knowledge Base called Glucose has been created
    And I start the client application
    And the displayed KB name is Glucose
    When I enter the following text into the chat panel:
      | Open thyroid |
    Then the chatbot response contains the following terms:
      | Did you mean | Thyroids |
    And the displayed KB name is Glucose
    When I enter the following text into the chat panel:
      | yes |
    Then the displayed KB name is now Thyroids

  Scenario: A knowledge base can be created and is opened
    Given I start the client application
    When I enter the following text into the chat panel:
      | Create a knowledge base called Glucose |
    Then the chatbot response contains the following terms:
      | Created | Glucose |
    And the displayed KB name is now Glucose

  Scenario: Creating a knowledge base whose name is taken is refused
    Given I start the client application
    When I enter the following text into the chat panel:
      | Create a knowledge base called thyroids |
    Then the chatbot response contains the following terms:
      | already exists |
    And the displayed KB name is Thyroids

  Scenario: Creating a knowledge base whose name resembles an existing one asks first
    Given I start the client application
    When I enter the following text into the chat panel:
      | Create a knowledge base called Thyroid |
    Then the chatbot response contains the following terms:
      | already | Thyroids | Create | Thyroid |
    And the displayed KB name is Thyroids
    When I enter the following text into the chat panel:
      | yes |
    Then the displayed KB name is now Thyroid

  Scenario: An empty knowledge base offers a demonstration case
    Given A Knowledge Base called Glucose has been created
    And I start the client application
    And the displayed KB name is Glucose
    Then the chatbot response contains the following terms:
      | has no cases | external information system | pathology case | minimal case |
    When I enter the following text into the chat panel:
      | The pathology case please |
    Then the chatbot response contains the following terms:
      | Added | Einstein |
    And I should see the case Einstein as the current case

  Scenario: The open knowledge base can be closed and another opened afterwards
    Given case Case1 for KB Thyroids is provided having data:
      | Sun | hot |
    And I start the client application
    When I enter the following text into the chat panel:
      | Close this knowledge base |
    Then the chatbot response contains the following terms:
      | Closed | Thyroids |
    And no knowledge base is shown as selected
    And the case list is hidden
    When I enter the following text into the chat panel:
      | Open Thyroids |
    Then the displayed KB name is now Thyroids
    And I should see the case Case1 as the current case

  Scenario: No knowledge base open invites the user to open or create one
    Given I start the client application
    When I enter the following text into the chat panel:
      | Close this knowledge base |
    Then the chatbot response contains the following terms:
      | No knowledge base is open | Thyroids | open | create |

  Scenario: Deleting a knowledge base requires confirmation
    Given A Knowledge Base called Unwanted has been created
    And I start the client application
    And the displayed KB name is Thyroids
    When I enter the following text into the chat panel:
      | Delete the knowledge base Unwanted |
    Then the chatbot response contains the following terms:
      | Delete | Unwanted | cannot be undone |
    When I enter the following text into the chat panel:
      | yes |
    Then the chatbot response contains the following terms:
      | Deleted | Unwanted |
    When I enter the following text into the chat panel:
      | List the knowledge bases |
    Then the chatbot response consists of the following lines:
      | Thyroids (open) |

  Scenario: Deleting a knowledge base is abandoned if not confirmed
    Given A Knowledge Base called Scratch has been created
    And I start the client application
    When I enter the following text into the chat panel:
      | Delete Scratch |
    Then the chatbot response contains the following terms:
      | cannot be undone |
    When I enter the following text into the chat panel:
      | No, leave it |
    And I enter the following text into the chat panel:
      | List the knowledge bases |
    Then the chatbot response contains the following terms:
      | Scratch |

  Scenario: Deleting the open knowledge base closes it
    Given A Knowledge Base called Scratch has been created
    And I start the client application
    And the displayed KB name is Scratch
    When I enter the following text into the chat panel:
      | Delete this knowledge base |
    And I enter the following text into the chat panel:
      | yes |
    Then no knowledge base is shown as selected

  Scenario: A knowledge base cannot be opened while a rule is being built
    Given A Knowledge Base called Glucose has been created
    And case Bondi for KB Glucose is provided having data:
      | Sun | hot |
    And I start the client application
    And the displayed KB name is Glucose
    And I start to build a rule to add the comment "Go to the beach." for case Bondi
    When I enter the following text into the chat panel:
      | Open Thyroids |
    Then the chatbot response contains the following terms:
      | finish or cancel the current rule |
    And the displayed KB name is Glucose

  Scenario: The open knowledge base can be renamed
    Given I start the client application
    And the displayed KB name is Thyroids
    When I enter the following text into the chat panel:
      | Rename this knowledge base to Thyroid Function |
    Then the chatbot response contains the following terms:
      | Renamed | Thyroids | Thyroid Function |
    And the displayed KB name is now "Thyroid Function"

  Scenario: Renaming to a name that is taken is refused
    Given A Knowledge Base called Zinc has been created
    And I start the client application
    And the displayed KB name is Thyroids
    When I enter the following text into the chat panel:
      | Rename this knowledge base to zinc |
    Then the chatbot response contains the following terms:
      | already exists |
    And the displayed KB name is Thyroids

  Scenario: The description of the open knowledge base can be set and read back
    Given I start the client application
    When I enter the following text into the chat panel:
      | Set the description to: A basic thyroid management KB. |
    Then the chatbot response contains the following terms:
      | Description | updated |
    And the KB description is:
    """
    A basic thyroid management KB.
    """
    When I enter the following text into the chat panel:
      | What is the description of this knowledge base? |
    Then the chatbot response contains the following terms:
      | A basic thyroid management KB. |