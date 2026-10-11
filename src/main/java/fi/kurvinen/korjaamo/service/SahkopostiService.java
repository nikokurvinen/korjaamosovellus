package fi.kurvinen.korjaamo.service;

import fi.kurvinen.korjaamo.domain.Kayttaja;
import fi.kurvinen.korjaamo.domain.Rooli;
import fi.kurvinen.korjaamo.repository.KayttajaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Sähköpostien lähetys (vaatimukset 26 ja 27). Kehityksessä ja Dockerissa viestit menevät
 * Mailpitiin (http://localhost:8025), joten oikeita sähköposteja ei lähde.
 */
@Service
public class SahkopostiService {

    private static final Logger log = LoggerFactory.getLogger(SahkopostiService.class);

    private final JavaMailSender mailSender;
    private final KayttajaRepository kayttajaRepository;
    private final String lahettaja;

    public SahkopostiService(JavaMailSender mailSender, KayttajaRepository kayttajaRepository,
                             @Value("${app.mail.from}") String lahettaja) {
        this.mailSender = mailSender;
        this.kayttajaRepository = kayttajaRepository;
        this.lahettaja = lahettaja;
    }

    /**
     * Vaatimus 26: ilmoitus kaikille ylläpitäjille uudesta käyttäjästä.
     * <ul>
     *   <li>{@code @TransactionalEventListener} (oletuksena AFTER_COMMIT): viesti lähtee vain, jos
     *       käyttäjän tallennus onnistui.</li>
     *   <li>{@code @Async}: rekisteröityjä ei odota SMTP-yhteyttä, eikä SMTP-virhe kaada
     *       rekisteröitymistä.</li>
     *   <li>Commitin jälkeen alkuperäinen transaktio on jo päättynyt, joten ylläpitäjien haku
     *       tarvitsee oman transaktion ({@code REQUIRES_NEW}).</li>
     * </ul>
     */
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void ilmoitaYllapidolle(KayttajaRekisteroitynyt tapahtuma) {
        List<String> osoitteet = kayttajaRepository.findByRoolitContaining(Rooli.ADMIN).stream()
                .filter(Kayttaja::isAktiivinen)
                .map(Kayttaja::getEmail)
                .toList();
        if (osoitteet.isEmpty()) {
            return;
        }
        laheta(osoitteet.toArray(String[]::new),
                "Korjaamo: uusi käyttäjä " + tapahtuma.kayttajatunnus(),
                """
                Hei ylläpitäjä,

                Korjaamo-sovellukseen on rekisteröitynyt uusi käyttäjä:

                  Käyttäjätunnus: %s
                  Nimi:           %s
                  Sähköposti:     %s

                Käyttäjä sai roolin USER. Voit muuttaa rooleja Käyttäjähallinta-sivulla.
                """.formatted(tapahtuma.kayttajatunnus(), tapahtuma.nimi(), tapahtuma.email()));
    }

    /** Vaatimus 27: salasanan palautuslinkki. */
    @Async
    public void lahetaPalautuslinkki(String email, String nimi, String linkki) {
        laheta(new String[]{email}, "Korjaamo: salasanan vaihto",
                """
                Hei %s,

                Saimme pyynnön vaihtaa Korjaamo-tunnuksesi salasana. Voit asettaa uuden salasanan
                alla olevasta linkistä. Linkki on voimassa 30 minuuttia ja toimii vain kerran.

                %s

                Jos et pyytänyt salasanan vaihtoa, voit jättää tämän viestin huomiotta.
                """.formatted(nimi, linkki));
    }

    private void laheta(String[] vastaanottajat, String aihe, String teksti) {
        SimpleMailMessage viesti = new SimpleMailMessage();
        viesti.setFrom(lahettaja);
        viesti.setTo(vastaanottajat);
        viesti.setSubject(aihe);
        viesti.setText(teksti);
        try {
            mailSender.send(viesti);
            log.info("Sähköposti lähetetty: '{}' → {}", aihe, String.join(", ", vastaanottajat));
        } catch (MailException e) {
            // Lokitetaan eikä heitetä: taustasäikeen poikkeus ei näkyisi kenellekään.
            log.warn("Sähköpostin lähetys epäonnistui: {}", e.getMessage());
        }
    }
}
