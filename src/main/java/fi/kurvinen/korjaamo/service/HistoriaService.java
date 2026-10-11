package fi.kurvinen.korjaamo.service;

import fi.kurvinen.korjaamo.domain.Ajoneuvo;
import fi.kurvinen.korjaamo.domain.Asiakas;
import fi.kurvinen.korjaamo.domain.Huoltotilaus;
import fi.kurvinen.korjaamo.domain.KorjaamoRevisio;
import fi.kurvinen.korjaamo.domain.Vakuutus;
import fi.kurvinen.korjaamo.domain.Varaosa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Muutoshistorian lukeminen Hibernate Enversistä (vaatimus 32).
 * <p>
 * {@code forRevisionsOfEntityWithChanges} palauttaa jokaisesta revisiosta entiteetin tilan,
 * revision tiedot, muutostyypin ja muuttuneiden kenttien nimet. Viimeinen on mahdollista, koska
 * {@code global_with_modified_flag=true} tallentaa jokaiselle kentälle oman _MOD-sarakkeen.
 */
@Service
public class HistoriaService {

    /** Audioitavat entiteetit ja niiden näyttönimet aikajanalla. */
    public static final Map<Class<?>, String> KOHTEET = new LinkedHashMap<>();

    static {
        KOHTEET.put(Huoltotilaus.class, "Huoltotilaus");
        KOHTEET.put(Ajoneuvo.class, "Ajoneuvo");
        KOHTEET.put(Asiakas.class, "Asiakas");
        KOHTEET.put(Vakuutus.class, "Vakuutus");
        KOHTEET.put(Varaosa.class, "Varaosa");
    }

    /** Teknisiä kenttiä ei näytetä käyttäjälle "muuttuneina", koska ne muuttuvat joka tallennuksessa. */
    private static final Set<String> PIILOTETUT = Set.of("version", "muokattu", "muokannut", "luotu", "luonut");

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<HistoriaMerkinta> viimeisimmat(Class<?> rajaus, int maksimi) {
        List<HistoriaMerkinta> kaikki = new ArrayList<>();
        for (Map.Entry<Class<?>, String> e : KOHTEET.entrySet()) {
            if (rajaus != null && !rajaus.equals(e.getKey())) continue;
            AuditQuery q = AuditReaderFactory.get(em).createQuery()
                    .forRevisionsOfEntityWithChanges(e.getKey(), true)
                    .addOrder(AuditEntity.revisionNumber().desc())
                    .setMaxResults(maksimi);
            kaikki.addAll(muunna(q.getResultList(), e.getValue()));
        }
        kaikki.sort(Comparator.comparing(HistoriaMerkinta::revisio).reversed());
        return kaikki.size() > maksimi ? kaikki.subList(0, maksimi) : kaikki;
    }

    @Transactional(readOnly = true)
    public List<HistoriaMerkinta> entiteetinHistoria(Class<?> tyyppi, Long id) {
        AuditQuery q = AuditReaderFactory.get(em).createQuery()
                .forRevisionsOfEntityWithChanges(tyyppi, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().desc());
        return muunna(q.getResultList(), KOHTEET.getOrDefault(tyyppi, tyyppi.getSimpleName()));
    }

    @SuppressWarnings("unchecked")
    private List<HistoriaMerkinta> muunna(List<?> rivit, String kohdetyyppi) {
        List<HistoriaMerkinta> tulos = new ArrayList<>();
        for (Object o : rivit) {
            Object[] r = (Object[]) o;
            Object entiteetti = r[0];
            KorjaamoRevisio rev = (KorjaamoRevisio) r[1];
            RevisionType tyyppi = (RevisionType) r[2];
            Set<String> muuttuneet = new TreeSet<>((Set<String>) r[3]);
            muuttuneet.removeAll(PIILOTETUT);
            Long id = entiteetti instanceof fi.kurvinen.korjaamo.domain.AbstractAuditableEntity a ? a.getId() : null;
            tulos.add(new HistoriaMerkinta(
                    rev.getId(),
                    LocalDateTime.ofInstant(rev.getAika(), ZoneId.systemDefault()),
                    rev.getKayttaja(),
                    kohdetyyppi,
                    id,
                    String.valueOf(entiteetti),
                    Toiminto.of(tyyppi),
                    tyyppi == RevisionType.MOD ? List.copyOf(muuttuneet) : List.of()));
        }
        return tulos;
    }

    public enum Toiminto {
        LISAYS("Lisäys"), MUUTOS("Muutos"), POISTO("Poisto");

        private final String nimi;

        Toiminto(String nimi) { this.nimi = nimi; }

        public String getNimi() { return nimi; }

        static Toiminto of(RevisionType t) {
            return switch (t) {
                case ADD -> LISAYS;
                case MOD -> MUUTOS;
                case DEL -> POISTO;
            };
        }
    }

    public record HistoriaMerkinta(int revisio, LocalDateTime aika, String kayttaja, String kohdetyyppi,
                                   Long kohdeId, String kohde, Toiminto toiminto, List<String> muuttuneetKentat) {
    }
}
