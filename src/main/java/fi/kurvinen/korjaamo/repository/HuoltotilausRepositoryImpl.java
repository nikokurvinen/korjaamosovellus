package fi.kurvinen.korjaamo.repository;

import fi.kurvinen.korjaamo.domain.Ajoneuvo;
import fi.kurvinen.korjaamo.domain.Asiakas;
import fi.kurvinen.korjaamo.domain.Huoltotilaus;
import fi.kurvinen.korjaamo.domain.Varaosa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Huoltotilausten dynaaminen haku Criteria API:lla (Suodattaminen, kohdat 1–5).
 * <p>
 * Periaate: jokainen hakuehto muuttuu {@link Predicate}-olioksi <b>vain, jos käyttäjä on antanut
 * sen</b>. Predikaatit kerätään listaan ja yhdistetään lopuksi AND-ehdoksi. Tyhjällä lomakkeella
 * lista on tyhjä, jolloin {@code where()} ei rajaa mitään ja haku palauttaa kaikki tilaukset.
 * <p>
 * Samaa predikaattien rakennusta käytetään sekä tulos- että count-kyselyssä, jotta Vaadinin lazy-grid
 * saa aina tulosjoukkoa vastaavan rivimäärän.
 */
public class HuoltotilausRepositoryImpl implements HuoltotilausRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Huoltotilaus> hae(HuoltoHakuehdot ehdot, int offset, int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Huoltotilaus> query = cb.createQuery(Huoltotilaus.class);
        Root<Huoltotilaus> h = query.from(Huoltotilaus.class);

        /*
         * Ajoneuvo ja omistaja näytetään tulosgridissä, joten ne haetaan fetch-joinilla samalla
         * kyselyllä. Fetch-olio on Hibernatessa myös Join, joten samaa liitosta voidaan käyttää
         * predikaateissa – erillinen join tuottaisi SQL:ään saman taulun kahteen kertaan.
         */
        @SuppressWarnings("unchecked")
        Join<Huoltotilaus, Ajoneuvo> ajoneuvo = (Join<Huoltotilaus, Ajoneuvo>) h.<Huoltotilaus, Ajoneuvo>fetch("ajoneuvo", JoinType.INNER);
        @SuppressWarnings("unchecked")
        Join<Ajoneuvo, Asiakas> omistaja = (Join<Ajoneuvo, Asiakas>) ajoneuvo.<Ajoneuvo, Asiakas>fetch("omistaja", JoinType.INNER);

        List<Predicate> predikaatit = rakennaPredikaatit(cb, h, ajoneuvo, omistaja, ehdot);

        /*
         * M:N-JOIN varaosiin voi monistaa saman tilauksen useaksi riviksi (yksi per osa).
         * distinct(true) palauttaa jokaisen tilauksen vain kerran.
         */
        query.select(h).distinct(true)
                .where(predikaatit.toArray(Predicate[]::new))
                .orderBy(cb.desc(h.get("vastaanotettu")), cb.desc(h.get("id")));

        return em.createQuery(query)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public long laske(HuoltoHakuehdot ehdot) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Huoltotilaus> h = query.from(Huoltotilaus.class);
        // Count-kyselyssä ei saa käyttää fetch-joinia (Hibernate hylkää sen), joten tässä tavallinen join.
        Join<Huoltotilaus, Ajoneuvo> ajoneuvo = h.join("ajoneuvo");
        Join<Ajoneuvo, Asiakas> omistaja = ajoneuvo.join("omistaja");

        List<Predicate> predikaatit = rakennaPredikaatit(cb, h, ajoneuvo, omistaja, ehdot);
        query.select(cb.countDistinct(h)).where(predikaatit.toArray(Predicate[]::new));
        return em.createQuery(query).getSingleResult();
    }

    private List<Predicate> rakennaPredikaatit(CriteriaBuilder cb, Root<Huoltotilaus> h,
                                               Join<Huoltotilaus, Ajoneuvo> ajoneuvo,
                                               Join<Ajoneuvo, Asiakas> omistaja,
                                               HuoltoHakuehdot e) {
        List<Predicate> p = new ArrayList<>();

        // --- Kohta 1: suodatus usean syötekentän perusteella (rekisterinumero, otsikko, hinta) ---
        if (onTeksti(e.rekisterinumero())) {
            p.add(cb.like(cb.upper(ajoneuvo.<String>get("rekisterinumero")), sisaltaa(e.rekisterinumero()).toUpperCase(Locale.ROOT)));
        }
        if (onTeksti(e.otsikko())) {
            p.add(cb.like(cb.lower(h.<String>get("otsikko")), sisaltaa(e.otsikko())));
        }
        if (e.minHinta() != null) {
            p.add(cb.greaterThanOrEqualTo(h.<BigDecimal>get("hinta"), e.minHinta()));
        }

        // --- Kohta 2: päivämääräväli. Toimii myös, jos vain toinen raja on annettu. ---
        if (e.alkaen() != null && e.asti() != null) {
            p.add(cb.between(h.<LocalDate>get("vastaanotettu"), e.alkaen(), e.asti()));
        } else if (e.alkaen() != null) {
            p.add(cb.greaterThanOrEqualTo(h.<LocalDate>get("vastaanotettu"), e.alkaen()));
        } else if (e.asti() != null) {
            p.add(cb.lessThanOrEqualTo(h.<LocalDate>get("vastaanotettu"), e.asti()));
        }

        // --- Kohta 3: suodatus relaatioentiteetin perusteella JOINilla (M:N varaosat) ---
        // Join luodaan vain tarvittaessa: turha liitos monistaisi rivejä ja hidastaisi kyselyä.
        if (e.varaosa() != null) {
            Join<Huoltotilaus, Varaosa> varaosat = h.join("varaosat");
            p.add(cb.equal(varaosat.get("id"), e.varaosa().getId()));
        }

        // --- Kohta 4: relaatioentiteetin ominaisuus: omistajan sukunimi (ajoneuvo → asiakas) ---
        if (onTeksti(e.asiakkaanSukunimi())) {
            p.add(cb.like(cb.lower(omistaja.<String>get("sukunimi")), sisaltaa(e.asiakkaanSukunimi())));
        }

        // --- Kohta 5: monimutkainen ehto (otsikko LIKE x OR kuvaus LIKE x) AND tila = y ---
        // OR-ryhmä muodostetaan omaksi predikaatikseen, jolloin se sulkeutuu SQL:ssä sulkuihin
        // ja AND sitoo tilaehdon koko ryhmään eikä vain kuvaukseen.
        if (onTeksti(e.hakusana())) {
            String sana = sisaltaa(e.hakusana());
            p.add(cb.or(
                    cb.like(cb.lower(h.<String>get("otsikko")), sana),
                    cb.like(cb.lower(cb.coalesce(h.<String>get("kuvaus"), "")), sana)));
        }
        if (e.tila() != null) {
            p.add(cb.equal(h.get("tila"), e.tila()));
        }
        return p;
    }

    private static boolean onTeksti(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Muuttaa hakusanan LIKE-muotoon %sana%. Käyttäjän syöttämät % ja _ ovat LIKE-jokerimerkkejä,
     * mutta niiden merkitys jätetään tarkoituksella voimaan (tehokäyttäjä voi hakea "A_C").
     * Arvo välitetään parametrina, joten SQL-injektio ei ole mahdollinen.
     */
    private static String sisaltaa(String s) {
        return "%" + s.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
