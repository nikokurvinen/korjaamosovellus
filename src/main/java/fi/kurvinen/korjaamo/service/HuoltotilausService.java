package fi.kurvinen.korjaamo.service;

import fi.kurvinen.korjaamo.domain.HuoltoTila;
import fi.kurvinen.korjaamo.domain.Huoltotilaus;
import fi.kurvinen.korjaamo.repository.HuoltoHakuehdot;
import fi.kurvinen.korjaamo.repository.HuoltotilausRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Huoltotilausten liiketoimintalogiikka: CRUD, työjonon tilasiirtymät, Criteria-haku ja
 * dashboardin tilastot.
 */
@Service
public class HuoltotilausService {

    private final HuoltotilausRepository repository;
    private final MuutosJulkaisija julkaisija;

    public HuoltotilausService(HuoltotilausRepository repository, MuutosJulkaisija julkaisija) {
        this.repository = repository;
        this.julkaisija = julkaisija;
    }

    @Transactional(readOnly = true)
    public List<Huoltotilaus> kaikki() {
        return repository.findAllByOrderByVastaanotettuDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Huoltotilaus> haeKokonaan(Long id) {
        return repository.findWithKaikkiById(id);
    }

    @Transactional(readOnly = true)
    public List<Huoltotilaus> tilassa(HuoltoTila tila) {
        return repository.findByTilaOrderByVastaanotettuAsc(tila);
    }

    /**
     * Criteria API -haku. Varaosat alustetaan transaktion sisällä, koska tulosgrid näyttää ne ja
     * open-in-view on pois päältä; ilman tätä gridin renderöinti kaatuisi LazyInitializationExceptioniin.
     */
    @Transactional(readOnly = true)
    public List<Huoltotilaus> hae(HuoltoHakuehdot ehdot, int offset, int limit) {
        List<Huoltotilaus> tulos = repository.hae(ehdot, offset, limit);
        tulos.forEach(h -> Hibernate.initialize(h.getVaraosat()));
        return tulos;
    }

    @Transactional(readOnly = true)
    public long laske(HuoltoHakuehdot ehdot) {
        return repository.laske(ehdot);
    }

    @Transactional
    public Huoltotilaus tallenna(Huoltotilaus tilaus) {
        if (tilaus.getTila() == HuoltoTila.VALMIS && tilaus.getValmistuu() == null) {
            tilaus.setValmistuu(LocalDate.now());
        }
        boolean uusi = tilaus.isNew();
        Huoltotilaus tallennettu = repository.save(tilaus);
        julkaisija.julkaise("Huoltotilaus", uusi ? "lisätty" : "muokattu",
                tallennettu.getAjoneuvo().getRekisterinumero() + ": " + tallennettu.getOtsikko(), tallennettu.getId());
        return tallennettu;
    }

    /** Työjonon "Siirrä eteenpäin" -toiminto. Erillinen metodi, koska vain tila muuttuu. */
    @Transactional
    public Huoltotilaus siirraSeuraavaan(Long id) {
        Huoltotilaus h = repository.findWithKaikkiById(id)
                .orElseThrow(() -> new LiiketoimintaVirhe("Tilausta ei löytynyt – se on ehkä poistettu"));
        HuoltoTila seuraava = h.getTila().seuraava();
        if (seuraava == null) {
            throw new LiiketoimintaVirhe("Tilaus on jo luovutettu asiakkaalle");
        }
        h.setTila(seuraava);
        if (seuraava == HuoltoTila.VALMIS && h.getValmistuu() == null) {
            h.setValmistuu(LocalDate.now());
        }
        julkaisija.julkaise("Huoltotilaus", "tila: " + seuraava.getNimi(),
                h.getAjoneuvo().getRekisterinumero() + ": " + h.getOtsikko(), h.getId());
        return h;
    }

    @Transactional
    public void poista(Huoltotilaus tilaus) {
        repository.deleteById(tilaus.getId());
        julkaisija.julkaise("Huoltotilaus", "poistettu", tilaus.getOtsikko());
    }

    // ---------- Dashboardin tilastot (Chart.js, vaatimus 33) ----------

    @Transactional(readOnly = true)
    public Map<HuoltoTila, Long> maaratTiloittain() {
        Map<HuoltoTila, Long> m = new EnumMap<>(HuoltoTila.class);
        for (HuoltoTila t : HuoltoTila.values()) {
            m.put(t, repository.countByTila(t));
        }
        return m;
    }

    /** Vastaanotetut tilaukset ja niiden yhteenlaskettu hinta kuukausittain, viimeiset 6 kk. */
    @Transactional(readOnly = true)
    public Map<YearMonth, Kuukausitilasto> kuukausitilastot() {
        YearMonth nyt = YearMonth.now();
        Map<YearMonth, Kuukausitilasto> m = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            m.put(nyt.minusMonths(i), new Kuukausitilasto(0, BigDecimal.ZERO));
        }
        LocalDate alku = nyt.minusMonths(5).atDay(1);
        for (Huoltotilaus h : repository.findByVastaanotettuGreaterThanEqual(alku)) {
            m.computeIfPresent(YearMonth.from(h.getVastaanotettu()),
                    (k, v) -> new Kuukausitilasto(v.maara() + 1, v.liikevaihto().add(h.getHinta())));
        }
        return m;
    }

    public record Kuukausitilasto(long maara, BigDecimal liikevaihto) {
    }
}
