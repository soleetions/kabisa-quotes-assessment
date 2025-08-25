package nl.kabisa.quotes.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import nl.kabisa.quotes.model.Quote;
import nl.kabisa.quotes.model.RankedQuote;
import nl.kabisa.quotes.service.QuotesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing quotes.
 */
@RestController
@RequestMapping("/api/rest/quotes")
@RequiredArgsConstructor
public class QuotesRestController {

    private final QuotesService quotesService;

    @GetMapping("/random")
    public ResponseEntity<Quote> getRandomQuote() {
        return ResponseEntity.ok(quotesService.getRandomQuote());
    }

    @PostMapping("/vote/{id}")
    public ResponseEntity<Void> upvoteQuote(@PathVariable Long id) {
        quotesService.upvoteQuote(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<RankedQuote>> getRanking() {
        return ResponseEntity.ok(quotesService.getRanking());
    }
}
