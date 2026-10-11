package fi.kurvinen.korjaamo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sähköpostit lähetetään taustasäikeessä ({@code @Async}), jotta hidas tai saavuttamaton
 * SMTP-palvelin ei jäädytä käyttöliittymää rekisteröitymisen aikana (vaatimus 26).
 * Ajastusta käytetään dashboardin live-kelloon ja vanhentuneiden tokenien siivoukseen.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
