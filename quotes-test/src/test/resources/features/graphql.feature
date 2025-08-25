Feature: Quotes GraphQL API

  Scenario: Retrieve a random quote
    Given The Quotes Service is available
    When I request a random quote via GraphQL
    Then I should receive a quote with text and author in the GraphQL response

  Scenario: Vote for a quote
    Given The Quotes Service is available
    And I request a random quote via GraphQL
    When I vote for that quote via GraphQL
    Then I should receive a confirmation of my vote in the GraphQL response
