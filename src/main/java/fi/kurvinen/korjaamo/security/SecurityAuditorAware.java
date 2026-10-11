package fi.kurvinen.korjaamo.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Kertoo Spring Data Auditingille, kuka muutoksen teki (vaatimus 30), ja Enversille saman tiedon
 * revisioon (vaatimus 31).
 * <p>
 * Kun kirjautunutta käyttäjää ei ole (esim. käynnistyksen demodata tai rekisteröityminen),
 * tekijäksi merkitään "järjestelmä". Tyhjä arvo historiassa näyttäisi virheeltä.
 */
@Component("auditorAware")
public class SecurityAuditorAware implements AuditorAware<String> {

    public static final String JARJESTELMA = "järjestelmä";

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(nykyinenKayttaja());
    }

    public static String nykyinenKayttaja() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return JARJESTELMA;
        }
        return auth.getName();
    }
}
