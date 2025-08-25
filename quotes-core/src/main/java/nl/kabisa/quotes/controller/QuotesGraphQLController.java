package nl.kabisa.quotes.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import nl.kabisa.quotes.model.Quote;
import nl.kabisa.quotes.model.RankedQuote;
import nl.kabisa.quotes.service.QuotesService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for managing quotes.
 */
@Controller
@RequiredArgsConstructor
public class QuotesGraphQLController {

    private final QuotesService service;

    @QueryMapping("random")
    public Quote getRandomQuote() {
        return service.getRandomQuote();
    }

    @QueryMapping("ranking")
    public List<RankedQuote> getRanking() {
        return service.getRanking();
    }

    @MutationMapping("vote")
    public Boolean upvoteQuote(@Argument Long id) {
        service.upvoteQuote(id);
        return true;
    }
}
