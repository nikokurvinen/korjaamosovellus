package fi.kurvinen.korjaamo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sovelluksen käynnistysluokka. Vaadinin AppShell-asetukset (teema, Push) ovat erillisessä
 * {@link fi.kurvinen.korjaamo.config.AppShell}-luokassa, jotta käynnistysluokka pysyy siistinä.
 */
@SpringBootApplication
public class KorjaamoApplication {

    public static void main(String[] args) {
        SpringApplication.run(KorjaamoApplication.class, args);
    }
}
