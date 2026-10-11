package fi.kurvinen.korjaamo.security;

import fi.kurvinen.korjaamo.domain.Kayttaja;
import fi.kurvinen.korjaamo.repository.KayttajaRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yhdistää oman {@link Kayttaja}-entiteetin Spring Securityyn (vaatimus 16).
 * <p>
 * Spring Security vertaa kirjautumislomakkeen salasanaa tässä palautettuun BCrypt-tiivisteeseen
 * {@code PasswordEncoder.matches()}-metodilla; selkokielistä salasanaa ei lueta kannasta missään
 * vaiheessa, koska sellaista ei ole tallennettu.
 */
@Service
public class KayttajaDetailsService implements UserDetailsService {

    private final KayttajaRepository repository;

    public KayttajaDetailsService(KayttajaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Kayttaja k = repository.findByKayttajatunnus(username.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Tuntematon käyttäjä"));
        return User.withUsername(k.getKayttajatunnus())
                .password(k.getSalasanaHash())
                .disabled(!k.isAktiivinen())
                // ROLE_-etuliite: @RolesAllowed("ADMIN") tarkistaa authorityn "ROLE_ADMIN".
                .authorities(k.getRoolit().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).toList())
                .build();
    }
}
