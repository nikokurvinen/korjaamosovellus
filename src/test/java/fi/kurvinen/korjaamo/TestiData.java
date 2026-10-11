package fi.kurvinen.korjaamo;

import fi.kurvinen.korjaamo.domain.Ajoneuvo;
import fi.kurvinen.korjaamo.domain.Asiakas;
import fi.kurvinen.korjaamo.domain.HuoltoTila;
import fi.kurvinen.korjaamo.domain.Huoltotilaus;
import fi.kurvinen.korjaamo.domain.Varaosa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/** Testien yhteiset olioiden rakentajat, jotta jokainen testi ei toista kenttien asettelua. */
final class TestiData {

    private TestiData() {
    }

    static Asiakas asiakas(String etunimi, String sukunimi, String email) {
        Asiakas a = new Asiakas();
        a.setEtunimi(etunimi);
        a.setSukunimi(sukunimi);
        a.setEmail(email);
        a.setPuhelin("+358 40 1234567");
        a.setKaupunki("Tampere");
        return a;
    }

    static Ajoneuvo ajoneuvo(String rekkari, Asiakas omistaja) {
        Ajoneuvo a = new Ajoneuvo();
        a.setRekisterinumero(rekkari);
        a.setMerkki("Toyota");
        a.setMalli("Corolla");
        a.setVuosimalli(2018);
        a.setKilometrit(100_000);
        a.setVin("JTDKB20U503456789".substring(0, 14) + String.format("%03d", Math.abs(rekkari.hashCode()) % 1000));
        a.setOmistaja(omistaja);
        return a;
    }

    static Varaosa varaosa(String tuotenumero, String nimi) {
        Varaosa v = new Varaosa();
        v.setTuotenumero(tuotenumero);
        v.setNimi(nimi);
        v.setValmistaja("Bosch");
        v.setHinta(new BigDecimal("19.90"));
        v.setVarastosaldo(10);
        return v;
    }

    static Huoltotilaus tilaus(String otsikko, String kuvaus, Ajoneuvo ajoneuvo, HuoltoTila tila,
                               LocalDate vastaanotettu, String hinta, Set<Varaosa> osat) {
        Huoltotilaus h = new Huoltotilaus();
        h.setOtsikko(otsikko);
        h.setKuvaus(kuvaus);
        h.setAjoneuvo(ajoneuvo);
        h.setTila(tila);
        h.setVastaanotettu(vastaanotettu);
        h.setHinta(new BigDecimal(hinta));
        h.setTyotunnit(BigDecimal.ONE);
        h.setVaraosat(osat);
        return h;
    }
}
