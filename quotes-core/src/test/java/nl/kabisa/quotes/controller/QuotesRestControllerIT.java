package nl.kabisa.quotes.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import nl.kabisa.quotes.model.Quote;
import nl.kabisa.quotes.repository.QuotesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;
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
@AutoConfigureMockMvc
class QuotesRestControllerIT {

    @InjectWireMock("dummy-json")
    WireMockServer wireMock;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuotesRepository repository;

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
    void noResponseAndEmptyCacheReturnsNotFound() throws Exception {
        this.mockMvc.perform(get("/api/rest/quotes/random"))
            .andExpect(status().isNotFound());
    }

    @DisplayName("""
        GIVEN a response from Dummy JSON
        WHEN a random quote is requested
        THEN the quote is returned
        AND the quote is requested only once from Dummy JSON
        """)
    @Test
    void responseFromDummyJsonIsReturnedByService() throws Exception {
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
        this.mockMvc.perform(get("/api/rest/quotes/random"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(3L))
            .andExpect(jsonPath("$.quote").value("If you want to lift yourself up, lift up someone else."))
            .andExpect(jsonPath("$.author").value("Booker T. Washington"));

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
    void noResponseFromDummyJsonReturnsQuoteFromCache() throws Exception {
        // GIVEN no response from Dummy JSON
        wireMock.stubFor(WireMock.get("/quotes/random").willReturn(notFound()));

        // AND quotes are present in the local cache
        repository.save(new Quote(1L, "Test quote 1", "Author 1"));

        // WHEN a random quote is requested
        // THEN a quote from the local cache is returned
        this.mockMvc.perform(get("/api/rest/quotes/random"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.quote").value("Test quote 1"))
            .andExpect(jsonPath("$.author").value("Author 1"));
    }

    @DisplayName("""
        GIVEN a quote is present in the local cache
        WHEN a vote is given for that quote
        THEN expect the amount of votes for that quote to be increased
        """)
    @Test
    void votingIncreasesVoteCount() throws Exception {
        // GIVEN a quote is present in the local cache
        var quote1 = repository.save(new Quote(1L, "Test quote 1", "Author 1"));
        var quote2 = repository.save(new Quote(2L, "Test quote 2", "Author 2"));

        // WHEN a vote is given for that quote
        this.mockMvc.perform(post("/api/rest/quotes/vote/{id}", quote2.id()))
            .andExpect(status().isOk());

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
        THEN expect a 404 response status
        """)
    @Test
    void votingForNonExistingQuoteReturns404() throws Exception {
        // GIVEN a quote is not present in the local cache

        // WHEN a vote is given for that quote
        // THEN expect a 404 response status
        this.mockMvc.perform(post("/api/rest/quotes/vote/999"))
            .andExpect(status().isNotFound());
    }

    @DisplayName("""
        GIVEN multiple quotes are present in the local cache
        AND votes have been given to some of the quotes
        WHEN the ranked quotes are requested
        THEN expect the top 10 quotes to be returned ordered by number of votes
        """)
    @Test
    void rankedQuotesAreOrderedByVotes() throws Exception {
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
        this.mockMvc.perform(post("/api/rest/quotes/vote/{id}", quote1.id())).andExpect(status().isOk());

        this.mockMvc.perform(post("/api/rest/quotes/vote/{id}", quote2.id())).andExpect(status().isOk());
        this.mockMvc.perform(post("/api/rest/quotes/vote/{id}", quote2.id())).andExpect(status().isOk());

        this.mockMvc.perform(post("/api/rest/quotes/vote/{id}", quote3.id())).andExpect(status().isOk());
        this.mockMvc.perform(post("/api/rest/quotes/vote/{id}", quote3.id())).andExpect(status().isOk());
        this.mockMvc.perform(post("/api/rest/quotes/vote/{id}", quote3.id())).andExpect(status().isOk());

        // WHEN the ranked quotes are requested
        // THEN expect the top 10 quotes to be returned ordered by number of votes
        this.mockMvc.perform(get("/api/rest/quotes/ranking"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(10))
            .andExpect(jsonPath("$[0].quote.id").value(quote3.id()))
            .andExpect(jsonPath("$[0].votes").value(3))
            .andExpect(jsonPath("$[1].quote.id").value(quote2.id()))
            .andExpect(jsonPath("$[1].votes").value(2))
            .andExpect(jsonPath("$[2].quote.id").value(quote1.id()))
            .andExpect(jsonPath("$[2].votes").value(1));
    }
}
