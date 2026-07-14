Feature: KB available as library component

  @single
  Scenario: An exported KB can be used in conjunction with the OpenRDR jar as an in-JVM interpreter.
    Given I start the client application
    And I create a Knowledge Base with the name CancerTypeClassifier
    And case Case1 for KB CancerTypeClassifier is provided having data:
      | Morphology  | Leiomyosarcoma, NOS  |
      | Topography  | Uterus, NOS          |
      | Cancer Type | Bone and Soft Tissue |
    When I select case Case1
    And I build a rule to add the comment "Uterine leiomyosarcoma" with conditions
      | Morphology contains "Leiomyosarcoma" |
      | Topography contains "Uterus"         |
    And  the interpretation should be "Uterine leiomyosarcoma"
    And I export the current Knowledge Base
    And An in-process interpreter using the exported kb gets the interpretation "Uterine leiomyosarcoma" for the input map
      | Morphology  | Leiomyosarcoma, not otherwise specified  |
      | Topography  | Uterus          |

