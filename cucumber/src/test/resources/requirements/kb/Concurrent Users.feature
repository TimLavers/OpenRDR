Feature: Several users share one server, each with their own chat and open knowledge base

  The users here talk to the server over REST with their own identities; no GUI is involved.

  Background:
    Given A Knowledge Base called Thyroids has been created
    And A Knowledge Base called Glucose has been created

  Scenario: Each user sees their own knowledge base as the open one
    Given Alice starts a conversation about the knowledge base Thyroids
    And Bob starts a conversation about the knowledge base Glucose
    When Alice says "List the knowledge bases" in the chat
    Then the chat response to Alice contains the following terms:
      | Thyroids (open) | Glucose |
    And the chat response to Alice does not contain "Glucose (open)"
    When Bob says "List the knowledge bases" in the chat
    Then the chat response to Bob contains the following terms:
      | Glucose (open) | Thyroids |
    And the chat response to Bob does not contain "Thyroids (open)"

  Scenario: Deleting a knowledge base nobody else has open leaves the other user's conversation intact
    Given Alice starts a conversation about the knowledge base Thyroids
    And Bob starts a conversation with no knowledge base open
    When Bob asks the chat to delete the knowledge base Glucose
    And Alice says "List the knowledge bases" in the chat
    Then the chat response to Alice contains the following terms:
      | Thyroids (open) |
    And the chat response to Alice does not contain "Glucose"

  Scenario: Deleting the knowledge base another user has open ends that user's conversation
    Given Alice starts a conversation about the knowledge base Thyroids
    And Bob starts a conversation with no knowledge base open
    When Bob asks the chat to delete the knowledge base Thyroids
    And Alice says "List the knowledge bases" in the chat
    Then the chat response to Alice contains the following terms:
      | No conversation has been started | open a knowledge base |

  Scenario: A knowledge base being edited by one user is refused to another
    Given case Case1 for KB Thyroids is provided having data:
      | TSH | 0.67 |
    When Alice starts a rule session on case Case1 in the knowledge base Thyroids
    Then Alice's request succeeds
    When Bob starts a rule session on case Case1 in the knowledge base Thyroids
    Then Bob's request is refused with "Thyroids is being edited by Alice."

  Scenario: Editing one knowledge base does not affect another
    Given case Case1 for KB Thyroids is provided having data:
      | TSH | 0.67 |
    And case Case2 for KB Glucose is provided having data:
      | Glucose | 5.1 |
    And Alice starts a rule session on case Case1 in the knowledge base Thyroids
    When Bob starts a rule session on case Case2 in the knowledge base Glucose
    Then Bob's request succeeds

  Scenario: A knowledge base being edited by one user can still be read by another
    Given case Case1 for KB Thyroids is provided having data:
      | TSH | 0.67 |
    And Alice starts a rule session on case Case1 in the knowledge base Thyroids
    When Bob reads case Case1 in the knowledge base Thyroids
    Then Bob's request succeeds

  Scenario: Closing a knowledge base lets another user edit it
    Given case Case1 for KB Thyroids is provided having data:
      | TSH | 0.67 |
    And Alice starts a conversation about the knowledge base Thyroids
    And Alice starts a rule session on case Case1 in the knowledge base Thyroids
    When Alice cancels her rule session
    And Alice asks the chat to close the knowledge base
    And Bob starts a rule session on case Case1 in the knowledge base Thyroids
    Then Bob's request succeeds

  Scenario: The chat tells a user who is editing the knowledge base they want to delete
    Given case Case1 for KB Thyroids is provided having data:
      | TSH | 0.67 |
    And Alice starts a conversation about the knowledge base Thyroids
    And Bob starts a conversation with no knowledge base open
    And Alice starts a rule session on case Case1 in the knowledge base Thyroids
    When Bob says "Delete the knowledge base Thyroids" in the chat
    And Bob says "yes" in the chat
    Then the chat response to Bob contains the following terms:
      | Thyroids is being edited by Alice |
    When Bob says "List the knowledge bases" in the chat
    Then the chat response to Bob contains the following terms:
      | Thyroids |
