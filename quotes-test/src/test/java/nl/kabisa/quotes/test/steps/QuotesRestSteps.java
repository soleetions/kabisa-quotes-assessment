package nl.kabisa.quotes.test.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Random;
import nl.kabisa.quotes.model.Quote;
import nl.kabisa.quotes.model.RankedQuote;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;

public class QuotesRestSteps {

    private final WebTestClient restClient = WebTestClient.bindToServer()
        .baseUrl("http://localhost:8080")
        .build();

    private ResponseSpec response;

    private final Random random = new Random();

    @Given("The Quotes Service is available")
    public void quotesServiceIsRunning() {
        restClient.get().uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP");
    }

    @When("I request a random quote")
    public void requestRandomQuote() {
        response = restClient.get()
            .uri("/api/rest/quotes/random")
            .exchange();
    }


    @Then("I should receive a quote with text and author")
    public void quoteShouldContainTextAndAuthor() {
        response.expectStatus().isOk()
            .expectBody()
            .jsonPath("$.quote").isNotEmpty()
            .jsonPath("$.author").isNotEmpty();
    }

    @When("I vote for that quote")
    public void voteForQuote() {
        final Quote quote = response.returnResult(Quote.class).getResponseBody().blockFirst();

        response = restClient.post()
            .uri("/api/rest/quotes/vote/{id}", quote.id())
            .exchange();
    }

    @Then("I should receive a confirmation of my vote")
    public void iShouldReceiveAConfirmationOfMyVote() {
        response.expectStatus().isOk();
    }

    @And("quotes have been voted on")
    public void quotesHaveBeenVotedOn() {
        for (long i = 1; i <= 15; i++) {
            // Get the id of a random quote
            var quote = restClient.get().uri("/api/rest/quotes/random")
                .exchange()
                .returnResult(Quote.class).getResponseBody()
                .blockFirst();

            // Vote a random number of times for that quote
            for (int j = 0; j < random.nextInt(5); j++) {
                restClient.post()
                    .uri("/api/rest/quotes/vote/{id}", quote.id())
                    .exchange()
                    .expectStatus().isOk();
            }
        }
    }

    @When("I request the quotes ranking")
    public void iRequestTheQuotesRanking() {
        response = restClient.get()
            .uri("/api/rest/quotes/ranking")
            .exchange()
            .expectStatus().isOk();
    }

    @Then("I should receive a list of {int} quotes with text and author")
    public void iShouldReceiveAListOfQuotesWithTextAndAuthor(int amountOfQuotes) {
        response.expectBodyList(RankedQuote.class)
            .hasSize(amountOfQuotes);
    }
}
