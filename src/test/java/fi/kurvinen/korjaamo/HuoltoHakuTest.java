package fi.kurvinen.korjaamo;

import fi.kurvinen.korjaamo.domain.Ajoneuvo;
import fi.kurvinen.korjaamo.domain.Asiakas;
import fi.kurvinen.korjaamo.domain.HuoltoTila;
import fi.kurvinen.korjaamo.domain.Huoltotilaus;
import fi.kurvinen.korjaamo.domain.Varaosa;
import fi.kurvinen.korjaamo.repository.AjoneuvoRepository;
import fi.kurvinen.korjaamo.repository.AsiakasRepository;
import fi.kurvinen.korjaamo.repository.HuoltoHakuehdot;
import fi.kurvinen.korjaamo.repository.HuoltotilausRepository;
import fi.kurvinen.korjaamo.repository.VaraosaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Criteria API -haun testit (Suodattaminen 1–5). Jokainen ehto testataan erikseen, ja lisäksi
 * varmistetaan, että tyhjät ehdot eivät rajaa tuloksia (predikaatteja lisätään vain tarvittaessa).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HuoltoHakuTest {

    @Autowired HuoltotilausRepository repository;
    @Autowired AsiakasRepository asiakkaat;
    @Autowired AjoneuvoRepository ajoneuvot;
    @Autowired VaraosaRepository varaosat;

    private Varaosa jarrupala;

    @BeforeEach
    void data() {
        Asiakas virtanen = asiakkaat.save(TestiData.asiakas("Matti", "Virtanen", "matti@example.fi"));
        Asiakas korhonen = asiakkaat.save(TestiData.asiakas("Liisa", "Korhonen", "liisa@example.fi"));
        Ajoneuvo abc = ajoneuvot.save(TestiData.ajoneuvo("ABC-123", virtanen));
        Ajoneuvo xyz = ajoneuvot.save(TestiData.ajoneuvo("XYZ-789", korhonen));
        jarrupala = varaosat.save(TestiData.varaosa("JAR-100", "Jarrupala"));
        Varaosa suodatin = varaosat.save(TestiData.varaosa("SUO-200", "Öljynsuodatin"));

        repository.save(TestiData.tilaus("Jarrujen uusinta", "Etujarrut kuluneet", abc, HuoltoTila.TYON_ALLA,
                LocalDate.of(2026, 3, 10), "250.00", Set.of(jarrupala)));
        repository.save(TestiData.tilaus("Öljynvaihto", "Pikahuolto, tarkasta myös jarrut", xyz, HuoltoTila.JONOSSA,
                LocalDate.of(2026, 5, 2), "90.00", Set.of(suodatin)));
        repository.save(TestiData.tilaus("Määräaikaishuolto", "Öljy ja suodattimet", abc, HuoltoTila.VALMIS,
                LocalDate.of(2026, 7, 20), "420.00", Set.of(suodatin, jarrupala)));
    }

    private List<Huoltotilaus> hae(HuoltoHakuehdot e) {
        return repository.hae(e, 0, 100);
    }

    @Test
    void tyhjatEhdotPalauttavatKaikki() {
        assertThat(hae(HuoltoHakuehdot.tyhja())).hasSize(3);
        assertThat(repository.laske(HuoltoHakuehdot.tyhja())).isEqualTo(3);
    }

    @Test
    void kohta1_useaSyotekentta() {
        var e = new HuoltoHakuehdot("abc", "huolto", new BigDecimal("300"), null, null, null, null, null, null);
        assertThat(hae(e)).extracting(Huoltotilaus::getOtsikko).containsExactly("Määräaikaishuolto");
    }

    @Test
    void kohta2_paivamaaravali() {
        var e = new HuoltoHakuehdot(null, null, null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31), null, null, null, null);
        assertThat(hae(e)).hasSize(2);
        var vainAlku = new HuoltoHakuehdot(null, null, null, LocalDate.of(2026, 7, 1), null, null, null, null, null);
        assertThat(hae(vainAlku)).hasSize(1);
    }

    @Test
    void kohta3_joinVaraosaanEiMonistaRiveja() {
        var e = new HuoltoHakuehdot(null, null, null, null, null, jarrupala, null, null, null);
        assertThat(hae(e)).hasSize(2).doesNotHaveDuplicates();
        assertThat(repository.laske(e)).isEqualTo(2);
    }

    @Test
    void kohta4_relaatioentiteetinOminaisuus() {
        var e = new HuoltoHakuehdot(null, null, null, null, null, null, "korh", null, null);
        assertThat(hae(e)).extracting(Huoltotilaus::getOtsikko).containsExactly("Öljynvaihto");
    }

    @Test
    void kohta5_orJaAnd() {
        // "jarru" löytyy kahden tilauksen otsikosta TAI kuvauksesta ...
        var vainOr = new HuoltoHakuehdot(null, null, null, null, null, null, null, "jarru", null);
        assertThat(hae(vainOr)).hasSize(2);
        // ... ja AND tila rajaa niistä yhden.
        var orJaAnd = new HuoltoHakuehdot(null, null, null, null, null, null, null, "jarru", HuoltoTila.JONOSSA);
        assertThat(hae(orJaAnd)).extracting(Huoltotilaus::getOtsikko).containsExactly("Öljynvaihto");
    }
}
