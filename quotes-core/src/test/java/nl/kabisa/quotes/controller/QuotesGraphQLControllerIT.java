package nl.kabisa.quotes.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import nl.kabisa.quotes.model.Quote;
import nl.kabisa.quotes.repository.QuotesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"quotes.dummy-json.url=http://localhost:${wiremock.server.port}/quotes"}
)
@EnableWireMock({
    @ConfigureWireMock(name = "dummy-json")
})
class QuotesGraphQLControllerIT {

    @InjectWireMock("dummy-json")
    WireMockServer wireMock;

    @Autowired
    private QuotesRepository repository;

    @Autowired
    private GraphQlTester graphQlTester;

    @BeforeEach
    void setup() {
        wireMock.resetAll();
        repository.flush();
    }

    @DisplayName("""
        GIVEN no response from Dummy JSON
        AND no quotes are present in the local cache
        WHEN a random quote is requested
        THEN expect an error indicating no quotes are available
        """)
    @Test
    void noResponseAndEmptyCacheReturnsNotFound() {
        graphQlTester.document("""
                {
                    random {
                        id
                        quote
                        author
                    }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors)
                .isNotEmpty()
                .hasSize(1)
                .anyMatch(error -> error.getMessage().contains("No quotes available"))
            );
    }

    @DisplayName("""
        GIVEN a response from Dummy JSON
        WHEN a random quote is requested
        THEN the quote is returned
        AND the quote is requested only once from Dummy JSON
        """)
    @Test
    void responseFromDummyJsonIsReturnedByService() {
        // GIVEN a response from Dummy JSON
        wireMock.stubFor(WireMock.get("/quotes/random").willReturn(okJson("""
            {
              "id": 3,
              "quote": "If you want to lift yourself up, lift up someone else.",
              "author": "Booker T. Washington"
            }
            """)));

        // WHEN a random quote is requested
        // THEN the quote is returned
        graphQlTester.document("""
                {
                    random {
                        id
                        quote
                        author
                    }
                }
                """)
            .execute()
            .path("random.id").entity(Long.class).isEqualTo(3L)
            .path("random.quote").entity(String.class).isEqualTo("If you want to lift yourself up, lift up someone else.")
            .path("random.author").entity(String.class).isEqualTo("Booker T. Washington");

        // AND the quote is requested only once from Dummy JSON
        wireMock.verify(1, getRequestedFor(urlEqualTo("/quotes/random")));
    }

    @DisplayName("""
        GIVEN no response from Dummy JSON
        AND quotes are present in the local cache
        WHEN a random quote is requested
        THEN a quote from the local cache is returned
        """)
    @Test
    void noResponseFromDummyJsonReturnsQuoteFromCache() {
        // GIVEN no response from Dummy JSON
        wireMock.stubFor(WireMock.get("/quotes/random").willReturn(notFound()));

        // AND quotes are present in the local cache
        repository.save(new Quote(1L, "Test quote 1", "Author 1"));

        // WHEN a random quote is requested
        // THEN a quote from the local cache is returned
        graphQlTester.document("""
                {
                    random {
                        id
                        quote
                        author
                    }
                }
                """)
            .execute()
            .path("random.id").entity(Long.class).isEqualTo(1L)
            .path("random.quote").entity(String.class).isEqualTo("Test quote 1")
            .path("random.author").entity(String.class).isEqualTo("Author 1");
    }

    @DisplayName("""
        GIVEN a quote is present in the local cache
        WHEN a vote is given for that quote
        THEN expect the amount of votes for that quote to be increased
        """)
    @Test
    void votingIncreasesVoteCount() {
        // GIVEN a quote is present in the local cache
        var quote1 = repository.save(new Quote(1L, "Test quote 1", "Author 1"));
        var quote2 = repository.save(new Quote(2L, "Test quote 2", "Author 2"));

        // WHEN a vote is given for that quote
        graphQlTester.document("""
                mutation($id: ID!) {
                  vote(id: $id)
                }
                """)
            .variable("id", quote2.id())
            .execute()
            .path("vote").entity(Boolean.class).isEqualTo(true);

        // THEN expect the amount of votes for that quote to be increased
        var rankedQuotes = repository.getRankedQuotesTop10();
        assertThat(rankedQuotes).hasSize(2);
        assertThat(rankedQuotes.getFirst().getQuote().id()).isEqualTo(quote2.id());
        assertThat(rankedQuotes.getFirst().getVotes().get()).isEqualTo(1);
        assertThat(rankedQuotes.getLast().getQuote().id()).isEqualTo(quote1.id());
    }

    @DisplayName("""
        GIVEN a quote is not present in the local cache
        WHEN a vote is given for that quote
        THEN expect an error response
        """)
    @Test
    void votingForNonExistingQuoteReturnsError() {
        // GIVEN a quote is not present in the local cache

        // WHEN a vote is given for that quote
        // THEN expect an error response
        graphQlTester.document("""
                mutation {
                  vote(id: "999")
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors)
                .isNotEmpty()
                .hasSize(1)
                .anyMatch(error -> error.getMessage().contains("Quote with id 999 not found"))
            );
    }

    @DisplayName("""
        GIVEN multiple quotes are present in the local cache
        AND votes have been given to some of the quotes
        WHEN the ranked quotes are requested
        THEN expect the top 10 quotes to be returned ordered by number of votes
        """)
    @Test
    void rankedQuotesAreOrderedByVotes() {
        // GIVEN multiple quotes are present in the local cache
        // Adding 3 specific quotes to be able to verify the order
        var quote1 = repository.save(new Quote(1L, "Test quote 1", "Author 1"));
        var quote2 = repository.save(new Quote(2L, "Test quote 2", "Author 2"));
        var quote3 = repository.save(new Quote(3L, "Test quote 3", "Author 3"));
        // Adding some extra quotes to ensure only the top 10 are returned
        for (int i = 4; i <= 15; i++) {
            repository.save(new Quote((long) i, "Test quote " + i, "Author " + i));
        }

        // AND votes have been given to some of the quotes
        graphQlTester.document("mutation($id: ID!) { vote(id: $id) }").variable("id", quote1.id()).execute();

        graphQlTester.document("mutation($id: ID!) { vote(id: $id) }").variable("id", quote2.id()).execute();
        graphQlTester.document("mutation($id: ID!) { vote(id: $id) }").variable("id", quote2.id()).execute();

        graphQlTester.document("mutation($id: ID!) { vote(id: $id) }").variable("id", quote3.id()).execute();
        graphQlTester.document("mutation($id: ID!) { vote(id: $id) }").variable("id", quote3.id()).execute();
        graphQlTester.document("mutation($id: ID!) { vote(id: $id) }").variable("id", quote3.id()).execute();

        // WHEN the ranked quotes are requested
        // THEN expect the top 10 quotes to be returned ordered by number of votes
        graphQlTester.document("""
                {
                  ranking {
                    quote {
                      id
                      quote
                      author
                    }
                    votes
                  }
                }
                """)
            .execute()
            .path("ranking").entityList(Object.class).hasSize(10)
            .path("ranking[0].quote.id").entity(Long.class).isEqualTo(quote3.id())
            .path("ranking[0].votes").entity(Integer.class).isEqualTo(3)
            .path("ranking[1].quote.id").entity(Long.class).isEqualTo(quote2.id())
            .path("ranking[1].votes").entity(Integer.class).isEqualTo(2)
            .path("ranking[2].quote.id").entity(Long.class).isEqualTo(quote1.id())
            .path("ranking[2].votes").entity(Integer.class).isEqualTo(1);
    }

}
