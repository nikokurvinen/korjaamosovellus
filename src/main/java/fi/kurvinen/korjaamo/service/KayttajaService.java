package fi.kurvinen.korjaamo.service;

import fi.kurvinen.korjaamo.domain.Kayttaja;
import fi.kurvinen.korjaamo.domain.Rooli;
import fi.kurvinen.korjaamo.repository.KayttajaRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Käyttäjien hallinta: rekisteröityminen (17), roolit (16), profiilikuva (19).
 */
@Service
public class KayttajaService {

    public static final int SALASANAN_MIN_PITUUS = 8;
    public static final long PROFIILIKUVAN_MAX_KOKO = 2 * 1024 * 1024;

    private final KayttajaRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher publisher;
    private final Validator validator;

    public KayttajaService(KayttajaRepository repository, PasswordEncoder passwordEncoder,
                           ApplicationEventPublisher publisher, Validator validator) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.publisher = publisher;
        this.validator = validator;
    }

    /**
     * Rekisteröi uuden käyttäjän USER-roolilla. Rooleja ei voi valita itse – muuten kuka tahansa
     * voisi rekisteröityä ylläpitäjäksi.
     * <p>
     * Ilmoitus ylläpitäjille julkaistaan eventtinä; varsinainen sähköposti lähtee vasta commitin
     * jälkeen ({@link SahkopostiService#ilmoitaYllapidolle}), jotta viestiä ei lähetetä käyttäjästä,
     * jonka tallennus epäonnistui.
     */
    @Transactional
    public Kayttaja rekisteroi(String kayttajatunnus, String nimi, String email, String salasana) {
        Kayttaja k = new Kayttaja();
        k.setKayttajatunnus(kayttajatunnus == null ? null : kayttajatunnus.trim().toLowerCase(Locale.ROOT));
        k.setNimi(nimi == null ? null : nimi.trim());
        k.setEmail(email == null ? null : email.trim().toLowerCase(Locale.ROOT));
        tarkistaSalasana(salasana);
        k.setSalasanaHash(passwordEncoder.encode(salasana));
        k.setRoolit(EnumSet.of(Rooli.USER));

        Set<ConstraintViolation<Kayttaja>> virheet = validator.validate(k);
        if (!virheet.isEmpty()) {
            throw new LiiketoimintaVirhe(virheet.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", ")));
        }
        if (repository.existsByKayttajatunnus(k.getKayttajatunnus())) {
            throw new LiiketoimintaVirhe("Käyttäjätunnus on jo varattu");
        }
        if (repository.existsByEmailIgnoreCase(k.getEmail())) {
            throw new LiiketoimintaVirhe("Sähköpostiosoitteella on jo tunnus");
        }
        Kayttaja tallennettu = repository.save(k);
        publisher.publishEvent(new KayttajaRekisteroitynyt(k.getKayttajatunnus(), k.getNimi(), k.getEmail()));
        return tallennettu;
    }

    /** Luo käyttäjän suoraan halutuilla rooleilla (demodata). */
    @Transactional
    public Kayttaja luo(String tunnus, String nimi, String email, String salasana, Set<Rooli> roolit) {
        Kayttaja k = new Kayttaja();
        k.setKayttajatunnus(tunnus);
        k.setNimi(nimi);
        k.setEmail(email);
        k.setSalasanaHash(passwordEncoder.encode(salasana));
        k.setRoolit(EnumSet.copyOf(roolit));
        return repository.save(k);
    }

    public static void tarkistaSalasana(String salasana) {
        if (salasana == null || salasana.length() < SALASANAN_MIN_PITUUS) {
            throw new LiiketoimintaVirhe("Salasanan on oltava vähintään " + SALASANAN_MIN_PITUUS + " merkkiä");
        }
    }

    @Transactional(readOnly = true)
    public List<Kayttaja> kaikki() {
        return repository.findAllByOrderByKayttajatunnusAsc();
    }

    @Transactional
    public Kayttaja paivitaRoolitJaTila(Long id, Set<Rooli> roolit, boolean aktiivinen) {
        if (roolit == null || roolit.isEmpty()) {
            throw new LiiketoimintaVirhe("Valitse vähintään yksi rooli");
        }
        Kayttaja k = repository.findById(id).orElseThrow();
        k.setRoolit(EnumSet.copyOf(roolit));
        k.setAktiivinen(aktiivinen);
        return k;
    }

    @Transactional
    public void poista(Long id, String poistaja) {
        Kayttaja k = repository.findById(id).orElseThrow();
        if (k.getKayttajatunnus().equals(poistaja)) {
            throw new LiiketoimintaVirhe("Et voi poistaa omaa tunnustasi");
        }
        repository.delete(k);
    }

    /**
     * Profiilikuvan tallennus (vaatimus 19). Tyyppi tarkistetaan palvelimella, vaikka Upload-
     * komponentti rajaa sen jo selaimessa – selaimen rajoituksen voi ohittaa helposti.
     */
    @Transactional
    public void asetaProfiilikuva(String kayttajatunnus, byte[] kuva, String tyyppi) {
        if (!"image/png".equals(tyyppi) && !"image/jpeg".equals(tyyppi)) {
            throw new LiiketoimintaVirhe("Sallitut kuvatyypit ovat PNG ja JPEG");
        }
        if (kuva.length > PROFIILIKUVAN_MAX_KOKO) {
            throw new LiiketoimintaVirhe("Kuva on liian suuri (max 2 Mt)");
        }
        Kayttaja k = repository.findByKayttajatunnus(kayttajatunnus).orElseThrow();
        k.setProfiilikuva(kuva);
        k.setProfiilikuvanTyyppi(tyyppi);
    }

    @Transactional
    public void poistaProfiilikuva(String kayttajatunnus) {
        repository.findByKayttajatunnus(kayttajatunnus).ifPresent(k -> {
            k.setProfiilikuva(null);
            k.setProfiilikuvanTyyppi(null);
        });
    }

    @Transactional
    public void paivitaTiedot(String kayttajatunnus, String nimi, String email) {
        Kayttaja k = repository.findByKayttajatunnus(kayttajatunnus).orElseThrow();
        String uusiEmail = email.trim().toLowerCase(Locale.ROOT);
        repository.findByEmailIgnoreCase(uusiEmail)
                .filter(o -> !o.getId().equals(k.getId()))
                .ifPresent(o -> {
                    throw new LiiketoimintaVirhe("Sähköpostiosoite on jo käytössä");
                });
        k.setNimi(nimi.trim());
        k.setEmail(uusiEmail);
        Set<ConstraintViolation<Kayttaja>> virheet = validator.validate(k);
        if (!virheet.isEmpty()) {
            throw new LiiketoimintaVirhe(virheet.iterator().next().getMessage());
        }
    }
}
