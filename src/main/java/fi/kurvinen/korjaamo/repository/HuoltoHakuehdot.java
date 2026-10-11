package fi.kurvinen.korjaamo.repository;

import fi.kurvinen.korjaamo.domain.HuoltoTila;
import fi.kurvinen.korjaamo.domain.Varaosa;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Hakunäkymän ehdot yhtenä muuttumattomana arvona. Kaikki kentät ovat valinnaisia: null tai tyhjä
 * merkkijono tarkoittaa, ettei ehtoa käytetä.
 *
 * @param rekisterinumero  osittainen rekisterinumero (LIKE, JOIN ajoneuvoon)
 * @param otsikko          osittainen otsikko (LIKE)
 * @param minHinta         hinta vähintään
 * @param alkaen           vastaanottopäivä alkaen
 * @param asti             vastaanottopäivä asti
 * @param varaosa          tilauksessa käytetty varaosa (JOIN M:N)
 * @param asiakkaanSukunimi omistajan sukunimi (JOIN ajoneuvo → asiakas)
 * @param hakusana         vapaa hakusana otsikosta TAI kuvauksesta
 * @param tila             tilauksen tila (yhdistetään hakusanaan AND-ehdolla)
 */
public record HuoltoHakuehdot(
        String rekisterinumero,
        String otsikko,
        BigDecimal minHinta,
        LocalDate alkaen,
        LocalDate asti,
        Varaosa varaosa,
        String asiakkaanSukunimi,
        String hakusana,
        HuoltoTila tila) {

    public static HuoltoHakuehdot tyhja() {
        return new HuoltoHakuehdot(null, null, null, null, null, null, null, null, null);
    }
}
