package nl.kabisa.quotes.test.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

public class QuotesGraphQlSteps {

    private final WebTestClient graphQlClient = WebTestClient
        .bindToServer()
        .baseUrl("http://localhost:8080/api/graphql")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .build();

    private WebTestClient.ResponseSpec response;

    @When("I request a random quote via GraphQL")
    public void requestRandomQuoteViaGraphQL() {
        String query = """
            {
              random {
                id
                quote
                author
              }
            }
            """;

        response = graphQlClient.post()
            .bodyValue("{\"query\":\"" + query.replace("\n", " ") + "\"}")
            .exchange()
            .expectStatus().isOk();
    }

    @Then("I should receive a quote with text and author in the GraphQL response")
    public void responseContainsTheCorrectValues() {
        response.expectBody()
            .jsonPath("$.data.random.id").isNotEmpty()
            .jsonPath("$.data.random.quote").isNotEmpty()
            .jsonPath("$.data.random.author").isNotEmpty();
    }

    @When("I vote for that quote via GraphQL")
    public void iVoteForThatQuoteViaGraphQL() {
        final Map<String, Object> responseBody = response.expectBody(Map.class).returnResult().getResponseBody();
        Map<String, Object> quote = (Map<String, Object>) ((Map<String, Object>) responseBody.get("data")).get("random");

        String document = """
            mutation($id: ID!) {
              vote(id: $id)
            }
            """;

        Map<String, Object> requestBody = Map.of(
            "query", document,
            "variables", Map.of("id", quote.get("id"))
        );
        response = graphQlClient.post()
            .bodyValue(requestBody)
            .exchange();
    }

    @Then("I should receive a confirmation of my vote in the GraphQL response")
    public void iShouldReceiveAConfirmationOfMyVoteInTheGraphQLResponse() {
        response.expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.vote").isEqualTo(true);
    }
}
