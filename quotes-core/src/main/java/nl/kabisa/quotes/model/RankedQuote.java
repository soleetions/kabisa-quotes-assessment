package nl.kabisa.quotes.model;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;

/**
 * A quote with an associated vote count.
 */
@Getter
public class RankedQuote {

    private final Quote quote;
    private final AtomicInteger votes = new AtomicInteger(0);

    public RankedQuote(Quote quote) {
        this.quote = quote;
    }

    public void upvote() {
        votes.incrementAndGet();
    }
}
