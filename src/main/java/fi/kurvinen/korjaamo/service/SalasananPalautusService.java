package fi.kurvinen.korjaamo.service;

import fi.kurvinen.korjaamo.domain.Kayttaja;
import fi.kurvinen.korjaamo.domain.SalasananPalautus;
import fi.kurvinen.korjaamo.repository.KayttajaRepository;
import fi.kurvinen.korjaamo.repository.SalasananPalautusRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Salasanan vaihto sähköpostin avulla (vaatimus 27).
 * <p>
 * Tietoturvaratkaisut:
 * <ul>
 *   <li>Token on 256-bittinen {@link SecureRandom}-arvo – arvaaminen on käytännössä mahdotonta.</li>
 *   <li>Kantaan tallennetaan vain tokenin SHA-256-tiiviste. BCrypt ei ole tässä tarpeen, koska
 *       tokenissa on täysi entropia eikä sitä voi murtaa sanakirjahyökkäyksellä.</li>
 *   <li>Token vanhenee 30 minuutissa ja on kertakäyttöinen.</li>
 *   <li>{@link #pyydaPalautus} ei kerro, löytyikö osoitetta – muuten lomakkeella voisi selvittää,
 *       kenellä on tunnus palvelussa (user enumeration).</li>
 * </ul>
 */
@Service
public class SalasananPalautusService {

    static final Duration VOIMASSAOLO = Duration.ofMinutes(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final KayttajaRepository kayttajaRepository;
    private final SalasananPalautusRepository repository;
    private final SahkopostiService sahkoposti;
    private final PasswordEncoder passwordEncoder;
    private final String baseUrl;

    public SalasananPalautusService(KayttajaRepository kayttajaRepository, SalasananPalautusRepository repository,
                                    SahkopostiService sahkoposti, PasswordEncoder passwordEncoder,
                                    @Value("${app.base-url}") String baseUrl) {
        this.kayttajaRepository = kayttajaRepository;
        this.repository = repository;
        this.sahkoposti = sahkoposti;
        this.passwordEncoder = passwordEncoder;
        this.baseUrl = baseUrl;
    }

    /** @return luotu token (testejä varten) tai tyhjä, jos osoitetta ei löytynyt */
    @Transactional
    public Optional<String> pyydaPalautus(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        Optional<Kayttaja> kayttaja = kayttajaRepository.findByEmailIgnoreCase(email.trim())
                .filter(Kayttaja::isAktiivinen);
        if (kayttaja.isEmpty()) {
            return Optional.empty();
        }
        Kayttaja k = kayttaja.get();
        repository.mitatoiAiemmat(k);

        byte[] satunnainen = new byte[32];
        RANDOM.nextBytes(satunnainen);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(satunnainen);
        repository.save(new SalasananPalautus(tiiviste(token), k, Instant.now().plus(VOIMASSAOLO)));

        sahkoposti.lahetaPalautuslinkki(k.getEmail(), k.getNimi(), baseUrl + "/salasana/uusi?token=" + token);
        return Optional.of(token);
    }

    @Transactional(readOnly = true)
    public boolean onKelvollinen(String token) {
        return token != null && repository.findByTokenHash(tiiviste(token))
                .map(t -> t.isKelvollinen(Instant.now()))
                .orElse(false);
    }

    @Transactional
    public void vaihdaSalasana(String token, String uusiSalasana) {
        KayttajaService.tarkistaSalasana(uusiSalasana);
        SalasananPalautus palautus = Optional.ofNullable(token)
                .flatMap(t -> repository.findByTokenHash(tiiviste(t)))
                .filter(t -> t.isKelvollinen(Instant.now()))
                .orElseThrow(() -> new LiiketoimintaVirhe("Linkki on vanhentunut tai jo käytetty. Pyydä uusi linkki."));
        palautus.getKayttaja().setSalasanaHash(passwordEncoder.encode(uusiSalasana));
        palautus.merkitseKaytetyksi();
    }

    static String tiiviste(String token) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 puuttuu JVM:stä", e);
        }
    }
}
