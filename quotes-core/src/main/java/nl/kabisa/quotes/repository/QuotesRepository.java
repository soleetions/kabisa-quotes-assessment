package nl.kabisa.quotes.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import nl.kabisa.quotes.model.Quote;
import nl.kabisa.quotes.model.RankedQuote;
import org.springframework.stereotype.Repository;

/**
 * Repository for caching quotes and maintaining their rankings.
 */
@Repository
public class QuotesRepository {

    private final Map<Long, RankedQuote> quotes = new ConcurrentHashMap<>();
    private final Random randomizer = new Random();

    /**
     * Saves a quote if it does not already exist.
     *
     * @param quote quote to save
     * @return the saved quote
     */
    public Quote save(Quote quote) {
        quotes.putIfAbsent(quote.id(), new RankedQuote(quote));
        return quote;
    }

    /**
     * Gets a random quote from the repository.
     *
     * @return random quote, or empty if no quotes are available
     */
    public Optional<Quote> getRandomQuote() {
        if (quotes.isEmpty()) {
            return Optional.empty();
        }

        return quotes.values().stream()
            .skip(randomizer.nextInt(quotes.size()))
            .findFirst()
            .map(RankedQuote::getQuote);
    }

    /**
     * Gets all quotes sorted by number of votes.
     *
     * @return list of ranked quotes
     */
    public List<RankedQuote> getRankedQuotesTop10() {
        return quotes.values().stream()
            .sorted((q1, q2) -> Integer.compare(q2.getVotes().get(), q1.getVotes().get()))
            .limit(10)
            .toList();
    }

    /**
     * Gets a ranked quote by its ID.
     *
     * @param id ID of the quote
     * @return optional ranked quote
     */
    public Optional<RankedQuote> getRankedQuote(Long id) {
        return Optional.ofNullable(quotes.get(id));
    }

    /**
     * Clears all quotes from the repository.
     */
    public void flush() {
        quotes.clear();
    }
}
