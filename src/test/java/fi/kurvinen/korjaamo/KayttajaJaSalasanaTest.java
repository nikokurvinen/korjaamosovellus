package fi.kurvinen.korjaamo;

import fi.kurvinen.korjaamo.domain.Kayttaja;
import fi.kurvinen.korjaamo.domain.Rooli;
import fi.kurvinen.korjaamo.repository.KayttajaRepository;
import fi.kurvinen.korjaamo.service.KayttajaRekisteroitynyt;
import fi.kurvinen.korjaamo.service.KayttajaService;
import fi.kurvinen.korjaamo.service.LiiketoimintaVirhe;
import fi.kurvinen.korjaamo.service.SalasananPalautusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/** Rekisteröinti (17), salasanan tiivisteet (16), ylläpidon ilmoitus (26) ja salasanan palautus (27). */
@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
class KayttajaJaSalasanaTest {

    @Autowired KayttajaService kayttajaService;
    @Autowired KayttajaRepository repository;
    @Autowired SalasananPalautusService palautusService;
    @Autowired PasswordEncoder encoder;
    @Autowired ApplicationEvents events;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void rekisterointiTallentaaVainTiivisteenJaLahettaaIlmoituksen() {
        kayttajaService.luo("yllapito", "Ylläpito", "yllapito@example.fi", "admin1234", Set.of(Rooli.ADMIN));
        Kayttaja k = kayttajaService.rekisteroi("Uusi.Kayttaja", "Uusi Käyttäjä", "uusi@example.fi", "salasana1");

        assertThat(k.getKayttajatunnus()).isEqualTo("uusi.kayttaja");
        assertThat(k.getRoolit()).containsExactly(Rooli.USER);
        assertThat(k.getSalasanaHash()).startsWith("$2a$").doesNotContain("salasana1");
        assertThat(encoder.matches("salasana1", k.getSalasanaHash())).isTrue();
        assertThat(events.stream(KayttajaRekisteroitynyt.class)).hasSize(1);
        // Viesti lähetetään asynkronisesti commitin jälkeen
        verify(mailSender, timeout(3000)).send(any(SimpleMailMessage.class));
    }

    @Test
    void samaTunnusEiKayToiseenKertaan() {
        kayttajaService.rekisteroi("tupla", "Tupla", "tupla@example.fi", "salasana1");
        assertThatThrownBy(() -> kayttajaService.rekisteroi("tupla", "Toinen", "toinen@example.fi", "salasana1"))
                .isInstanceOf(LiiketoimintaVirhe.class);
    }

    @Test
    void palautustokenToimiiVainKerran() {
        kayttajaService.rekisteroi("unohtaja", "Unohtaja", "unohtaja@example.fi", "vanha1234");
        String token = palautusService.pyydaPalautus("unohtaja@example.fi").orElseThrow();

        assertThat(palautusService.onKelvollinen(token)).isTrue();
        palautusService.vaihdaSalasana(token, "uusi12345");
        assertThat(encoder.matches("uusi12345", repository.findByKayttajatunnus("unohtaja").orElseThrow().getSalasanaHash())).isTrue();

        assertThat(palautusService.onKelvollinen(token)).isFalse();
        assertThatThrownBy(() -> palautusService.vaihdaSalasana(token, "kolmas1234")).isInstanceOf(LiiketoimintaVirhe.class);
    }

    @Test
    void tuntematonOsoiteEiPaljastuEikaLuoTokenia() {
        assertThat(palautusService.pyydaPalautus("ei-ole@example.fi")).isEmpty();
    }
}
