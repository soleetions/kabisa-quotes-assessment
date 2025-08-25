package nl.kabisa.quotes.client;

import lombok.RequiredArgsConstructor;
import nl.kabisa.quotes.model.Quote;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Client for interacting with the Dummy JSON API to fetch quotes.
 */
@Component
@RequiredArgsConstructor
public class DummyJsonClient {

    private final WebClient dummyJsonWebClient;

    /**
     * Fetches a random quote from the Dummy JSON API.
     *
     * @return a Mono emitting the fetched Quote
     */
    public Mono<Quote> getRandomQuote() {
        return dummyJsonWebClient.get()
            .uri("/random")
            .retrieve()
            .bodyToMono(Quote.class);
    }

}
