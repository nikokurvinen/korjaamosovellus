package fi.kurvinen.korjaamo.security;

import com.vaadin.flow.spring.security.AuthenticationContext;
import fi.kurvinen.korjaamo.domain.Kayttaja;
import fi.kurvinen.korjaamo.repository.KayttajaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Vaadin-näkymien apuluokka kirjautuneen käyttäjän tietoihin ja uloskirjautumiseen.
 * {@link AuthenticationContext} on Vaadinin suosittelema tapa: se toimii myös Push-säikeissä,
 * joissa {@code SecurityContextHolder} ei välttämättä ole asetettu.
 */
@Component
public class KirjautunutKayttaja {

    private final AuthenticationContext authenticationContext;
    private final KayttajaRepository repository;

    public KirjautunutKayttaja(AuthenticationContext authenticationContext, KayttajaRepository repository) {
        this.authenticationContext = authenticationContext;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<Kayttaja> get() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(u -> repository.findByKayttajatunnus(u.getUsername()));
    }

    public boolean onKirjautunut() {
        return authenticationContext.isAuthenticated();
    }

    public boolean onRooli(String rooli) {
        return authenticationContext.hasRole(rooli);
    }

    public void kirjauduUlos() {
        authenticationContext.logout();
    }
}
