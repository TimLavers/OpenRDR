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
