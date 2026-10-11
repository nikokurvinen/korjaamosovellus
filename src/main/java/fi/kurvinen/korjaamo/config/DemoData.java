package fi.kurvinen.korjaamo.config;

import fi.kurvinen.korjaamo.domain.Ajoneuvo;
import fi.kurvinen.korjaamo.domain.Asiakas;
import fi.kurvinen.korjaamo.domain.HuoltoTila;
import fi.kurvinen.korjaamo.domain.Huoltotilaus;
import fi.kurvinen.korjaamo.domain.Rooli;
import fi.kurvinen.korjaamo.domain.Vakuutus;
import fi.kurvinen.korjaamo.domain.VakuutusTyyppi;
import fi.kurvinen.korjaamo.domain.Varaosa;
import fi.kurvinen.korjaamo.repository.AjoneuvoRepository;
import fi.kurvinen.korjaamo.repository.AsiakasRepository;
import fi.kurvinen.korjaamo.repository.HuoltotilausRepository;
import fi.kurvinen.korjaamo.repository.KayttajaRepository;
import fi.kurvinen.korjaamo.repository.VakuutusRepository;
import fi.kurvinen.korjaamo.repository.VaraosaRepository;
import fi.kurvinen.korjaamo.service.KayttajaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Luo käynnistyksessä demokäyttäjät (vaatimus 16) ja esimerkkidatan, jos kanta on tyhjä.
 * <p>
 * Käyttäjät: admin/admin123 (ADMIN), super/super123 (SUPER), user/user123 (USER). Salasanat
 * tallennetaan {@link KayttajaService#luo}-metodin kautta BCrypt-tiivisteinä, joten selkokielinen
 * salasana esiintyy vain tässä lähdekoodissa demotarkoitusta varten.
 * <p>
 * Testeissä ({@code app.demo-data=false}) dataa ei luoda, jotta testit hallitsevat tilaa itse.
 */
@Component
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true", matchIfMissing = true)
public class DemoData implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoData.class);

    private final KayttajaRepository kayttajat;
    private final KayttajaService kayttajaService;
    private final AsiakasRepository asiakkaat;
    private final AjoneuvoRepository ajoneuvot;
    private final VakuutusRepository vakuutukset;
    private final VaraosaRepository varaosat;
    private final HuoltotilausRepository tilaukset;

    public DemoData(KayttajaRepository kayttajat, KayttajaService kayttajaService, AsiakasRepository asiakkaat,
                    AjoneuvoRepository ajoneuvot, VakuutusRepository vakuutukset, VaraosaRepository varaosat,
                    HuoltotilausRepository tilaukset) {
        this.kayttajat = kayttajat;
        this.kayttajaService = kayttajaService;
        this.asiakkaat = asiakkaat;
        this.ajoneuvot = ajoneuvot;
        this.vakuutukset = vakuutukset;
        this.varaosat = varaosat;
        this.tilaukset = tilaukset;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (kayttajat.count() == 0) {
            kayttajaService.luo("admin", "Aino Ylläpitäjä", "admin@korjaamo.local", "admin123", Set.of(Rooli.ADMIN));
            kayttajaService.luo("super", "Seppo Korjaamopäällikkö", "super@korjaamo.local", "super123", Set.of(Rooli.SUPER));
            kayttajaService.luo("user", "Ville Mekaanikko", "user@korjaamo.local", "user123", Set.of(Rooli.USER));
            log.info("Demokäyttäjät luotu: admin/admin123, super/super123, user/user123");
        }
        if (asiakkaat.count() > 0) {
            return;
        }
        Random r = new Random(42); // kiinteä siemen: sama demodata joka asennuksessa

        String[][] as = {
                {"Matti", "Virtanen", "matti.virtanen@example.fi", "+358 40 111 2233", "Hämeenkatu 12", "33100", "Tampere"},
                {"Liisa", "Korhonen", "liisa.korhonen@example.fi", "+358 50 222 3344", "Aleksanterinkatu 5", "00100", "Helsinki"},
                {"Jari", "Mäkinen", "jari.makinen@example.fi", "+358 44 333 4455", "Puistokatu 3", "40100", "Jyväskylä"},
                {"Sanna", "Nieminen", "sanna.nieminen@example.fi", "+358 40 444 5566", "Rantatie 8", "90100", "Oulu"},
                {"Pekka", "Mäkelä", "pekka.makela@example.fi", "+358 50 555 6677", "Koulukatu 21", "20100", "Turku"},
                {"Anna", "Hämäläinen", "anna.hamalainen@example.fi", "+358 45 666 7788", "Kauppakatu 2", "70100", "Kuopio"},
                {"Timo", "Laine", "timo.laine@example.fi", "+358 40 777 8899", "Asemakatu 14", "15100", "Lahti"},
                {"Kaisa", "Heikkinen", "kaisa.heikkinen@example.fi", "+358 50 888 9900", "Linnankatu 9", "53100", "Lappeenranta"},
        };
        List<Asiakas> asiakasLista = new ArrayList<>();
        for (String[] a : as) {
            Asiakas x = new Asiakas();
            x.setEtunimi(a[0]);
            x.setSukunimi(a[1]);
            x.setEmail(a[2]);
            x.setPuhelin(a[3]);
            x.setKatuosoite(a[4]);
            x.setPostinumero(a[5]);
            x.setKaupunki(a[6]);
            asiakasLista.add(asiakkaat.save(x));
        }

        Object[][] au = {
                {"ABC-123", "Toyota", "Corolla 1.8 Hybrid", 2019, 98_400, "JTDKB20U503456789", 0},
                {"XYZ-789", "Volkswagen", "Golf Variant", 2016, 187_200, "WVWZZZAUZGW123456", 0},
                {"KLM-456", "Skoda", "Octavia Combi", 2021, 54_300, "TMBJG7NE5M0123456", 1},
                {"BMW-1", "BMW", "320d Touring", 2018, 142_000, "WBA8E11030K123456", 2},
                {"VOL-240", "Volvo", "V60 D4", 2017, 165_800, "YV1ZW72UDH1234567", 3},
                {"FIN-100", "Kia", "Ceed SW", 2020, 61_900, "U5YH2512AL1234567", 4},
                {"TES-3", "Tesla", "Model 3 LR", 2022, 38_200, "5YJ3E7EB4NF123456", 5},
                {"MZD-626", "Mazda", "CX-5 2.0", 2015, 211_400, "JMZKE6W2A01234567", 5},
                {"OPL-12", "Opel", "Astra Sports Tourer", 2014, 232_100, "W0LPE8EC4E1234567", 6},
                {"FRD-77", "Ford", "Focus 1.0 EcoBoost", 2018, 120_600, "WF0NXXGCHNJA12345", 7},
                {"NSN-88", "Nissan", "Leaf 40 kWh", 2019, 76_500, "SJNFAAZE1U1234567", 1},
                {"HND-55", "Honda", "Civic 1.5 VTEC", 2017, 133_900, "SHHFK7860HU123456", 3},
        };
        List<Ajoneuvo> ajoneuvoLista = new ArrayList<>();
        for (Object[] a : au) {
            Ajoneuvo x = new Ajoneuvo();
            x.setRekisterinumero((String) a[0]);
            x.setMerkki((String) a[1]);
            x.setMalli((String) a[2]);
            x.setVuosimalli((Integer) a[3]);
            x.setKilometrit((Integer) a[4]);
            x.setVin((String) a[5]);
            x.setOmistaja(asiakasLista.get((Integer) a[6]));
            ajoneuvoLista.add(ajoneuvot.save(x));
        }

        String[] yhtiot = {"If", "LähiTapiola", "OP Vakuutus", "Fennia", "Pohjantähti"};
        // Kahdella viimeisellä ajoneuvolla ei ole vakuutusta, jotta 1:1-lomakkeella on valittavaa.
        for (int i = 0; i < ajoneuvoLista.size() - 2; i++) {
            Vakuutus v = new Vakuutus();
            v.setAjoneuvo(ajoneuvoLista.get(i));
            v.setYhtio(yhtiot[i % yhtiot.length]);
            v.setVakuutusnumero(String.format("%s%08d", yhtiot[i % yhtiot.length].substring(0, 2).toUpperCase().replace('Ä', 'A'), 10_000_000 + i * 7919));
            v.setTyyppi(VakuutusTyyppi.values()[i % 3]);
            LocalDate alku = LocalDate.now().minusMonths(2 + i * 2L);
            v.setAlkaen(alku);
            v.setPaattyy(i == 4 ? LocalDate.now().minusDays(10) : alku.plusYears(1));
            v.setOmavastuu(BigDecimal.valueOf(new int[]{150, 200, 300, 500}[i % 4]));
            vakuutukset.save(v);
        }

        Object[][] vo = {
                {"JAR-1042", "Jarrupalasarja etu", "Brembo", "64.90", 24},
                {"JAR-2210", "Jarrulevy etu (pari)", "Brembo", "119.00", 8},
                {"OLJ-0530", "Moottoriöljy 5W-30 5 l", "Castrol", "54.90", 40},
                {"SUO-110", "Öljynsuodatin", "Mann-Filter", "12.50", 35},
                {"SUO-220", "Ilmansuodatin", "Mann-Filter", "18.90", 22},
                {"SUO-330", "Raitisilmasuodatin", "Bosch", "21.40", 3},
                {"SYT-400", "Sytytystulppa (4 kpl)", "NGK", "39.60", 16},
                {"AKK-7400", "Akku 74 Ah", "Varta", "139.00", 6},
                {"PYY-550", "Pyyhkijänsulat (pari)", "Bosch", "29.90", 18},
                {"HIH-9001", "Jakohihnasarja + vesipumppu", "Gates", "289.00", 2},
                {"ISK-310", "Iskunvaimennin etu", "Monroe", "89.00", 4},
                {"POL-120", "Polttoainesuodatin", "Mahle", "34.50", 9},
                {"LAM-707", "Polttimo H7", "Philips", "9.90", 50},
                {"KYT-500", "Kytkinsarja", "Sachs", "349.00", 1},
                {"JAH-150", "Jäähdytinneste 4 l", "Motul", "26.90", 12},
        };
        List<Varaosa> osat = new ArrayList<>();
        for (Object[] o : vo) {
            Varaosa v = new Varaosa();
            v.setTuotenumero((String) o[0]);
            v.setNimi((String) o[1]);
            v.setValmistaja((String) o[2]);
            v.setHinta(new BigDecimal((String) o[3]));
            v.setVarastosaldo((Integer) o[4]);
            osat.add(varaosat.save(v));
        }

        String[][] tyot = {
                {"Määräaikaishuolto 60 000 km", "Öljynvaihto, suodattimet ja tarkastus valmistajan ohjelman mukaan."},
                {"Jarrujen uusinta edestä", "Etujarrupalat ja -levyt kuluneet, asiakas kuullut kirskuntaa jarrutettaessa."},
                {"Akun vaihto", "Auto ei käynnistynyt pakkasella, akku testattu heikoksi."},
                {"Jakohihnan vaihto", "Jakohihna ja vesipumppu vaihtovälissä."},
                {"Katsastukseen valmistelu", "Valot, pyyhkijät ja jarrujen tasapaino tarkastettava ennen katsastusta."},
                {"Kolahtelu etujousituksessa", "Kolinaa töyssyissä, epäily iskunvaimentimesta tai kallistuksenvakaajan tukivarresta."},
                {"Ilmastoinnin huolto", "Raitisilmasuodatin ja ilmastoinnin desinfiointi, huono haju."},
                {"Kytkin luistaa", "Kytkin luistaa kiihdytettäessä, kytkinsarjan vaihto."},
                {"Öljynvaihto", "Pikahuolto: öljy ja öljynsuodatin."},
                {"Vikakoodin diagnoosi", "Moottorin vikavalo palaa, vikakoodien luku ja selvitys."},
        };
        int[][] tyonOsat = {{2, 3, 4}, {0, 1}, {7}, {9}, {12, 8}, {10}, {5}, {13}, {2, 3}, {}};

        for (int i = 0; i < 28; i++) {
            int t = i % tyot.length;
            Huoltotilaus h = new Huoltotilaus();
            h.setOtsikko(tyot[t][0]);
            h.setKuvaus(tyot[t][1]);
            h.setAjoneuvo(ajoneuvoLista.get(r.nextInt(ajoneuvoLista.size())));
            LocalDate vastaanotettu = LocalDate.now().minusDays(i < 8 ? r.nextInt(6) : 7 + r.nextInt(170));
            h.setVastaanotettu(vastaanotettu);
            HuoltoTila tila = i < 8 ? HuoltoTila.values()[i % 3] : (r.nextInt(5) == 0 ? HuoltoTila.VALMIS : HuoltoTila.LUOVUTETTU);
            h.setTila(tila);
            if (tila == HuoltoTila.VALMIS || tila == HuoltoTila.LUOVUTETTU) {
                h.setValmistuu(vastaanotettu.plusDays(1 + r.nextInt(4)));
            } else {
                h.setValmistuu(vastaanotettu.plusDays(2 + r.nextInt(5)));
            }
            Set<Varaosa> kaytetyt = new HashSet<>();
            BigDecimal osienHinta = BigDecimal.ZERO;
            for (int o : tyonOsat[t]) {
                kaytetyt.add(osat.get(o));
                osienHinta = osienHinta.add(osat.get(o).getHinta());
            }
            h.setVaraosat(kaytetyt);
            BigDecimal tunnit = BigDecimal.valueOf(5 + r.nextInt(40), 1);
            h.setTyotunnit(tunnit);
            h.setHinta(tunnit.multiply(BigDecimal.valueOf(89)).add(osienHinta).setScale(2, java.math.RoundingMode.HALF_UP));
            tilaukset.save(h);
        }
        log.info("Demodata luotu: {} asiakasta, {} ajoneuvoa, {} varaosaa, 28 huoltotilausta",
                asiakasLista.size(), ajoneuvoLista.size(), osat.size());
    }
}
