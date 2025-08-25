package nl.kabisa.quotes.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for setting up the WebClient to interact with the Dummy JSON API.
 */
@Configuration
public class DummyJsonWebClientConfig {

    @Value("${quotes.dummy-json.url}")
    private String baseUrl;

    @Bean
    public WebClient dummyJsonWebClient() {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }
}
