Feature: Quotes REST API

  Scenario: Retrieve a random quote
    Given The Quotes Service is available
    When I request a random quote
    Then I should receive a quote with text and author

  Scenario: Vote for a quote
    Given The Quotes Service is available
    And I request a random quote
    When I vote for that quote
    Then I should receive a confirmation of my vote

  Scenario: Get the top 10 quotes
    Given The Quotes Service is available
    And quotes have been voted on
    When I request the quotes ranking
    Then I should receive a list of 10 quotes with text and author
