package nl.kabisa.quotes.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.kabisa.quotes.client.DummyJsonClient;
import nl.kabisa.quotes.exception.ResourceNotFoundException;
import nl.kabisa.quotes.model.Quote;
import nl.kabisa.quotes.model.RankedQuote;
import nl.kabisa.quotes.repository.QuotesRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service class for managing quotes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuotesService {

    private final DummyJsonClient dummyJsonClient;
    private final QuotesRepository quotesRepository;

    /**
     * Fetches a random quote from the external service and saves it to the repository. If the external service is unavailable, it falls back to a random quote from the local cache.
     *
     * @return a random quote
     * @throws ResourceNotFoundException if no quotes are available
     */
    public Quote getRandomQuote() {
        return dummyJsonClient.getRandomQuote()
            .map(quotesRepository::save)
            .onErrorResume(ex -> {
                log.warn("No quotes received form server, using local cache as fallback", ex);
                return quotesRepository.getRandomQuote()
                    .map(Mono::just)
                    .orElseThrow(() -> new ResourceNotFoundException("No quotes available"));
            })
            .block();
    }

    /**
     * Upvotes a quote by its ID.
     *
     * @param id ID of the quote to upvote
     * @throws ResourceNotFoundException if the quote with the given ID is not found
     */
    public void upvoteQuote(Long id) {
        quotesRepository.getRankedQuote(id)
            .orElseThrow(() -> new ResourceNotFoundException("Quote with id " + id + " not found"))
            .upvote();
    }

    /**
     * Retrieves the top 10 ranked quotes based on the number of votes.
     *
     * @return list of top 10 ranked quotes
     */
    public List<RankedQuote> getRanking() {
        return quotesRepository.getRankedQuotesTop10();
    }
}
