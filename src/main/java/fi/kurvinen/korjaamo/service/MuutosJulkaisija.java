package fi.kurvinen.korjaamo.service;

import fi.kurvinen.korjaamo.security.SecurityAuditorAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Pieni apuluokka, jotta palveluiden ei tarvitse koota {@link MuutosTapahtuma}a itse. */
@Component
public class MuutosJulkaisija {

    private final ApplicationEventPublisher publisher;

    public MuutosJulkaisija(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void julkaise(String kohde, String toiminto, String kuvaus) {
        julkaise(kohde, toiminto, kuvaus, null);
    }

    public void julkaise(String kohde, String toiminto, String kuvaus, Long kohdeId) {
        publisher.publishEvent(new MuutosTapahtuma(kohde, toiminto, kuvaus,
                SecurityAuditorAware.nykyinenKayttaja(), LocalDateTime.now(), kohdeId));
    }
}
