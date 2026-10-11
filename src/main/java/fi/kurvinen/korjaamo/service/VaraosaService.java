package fi.kurvinen.korjaamo.service;

import fi.kurvinen.korjaamo.domain.Varaosa;
import fi.kurvinen.korjaamo.repository.HuoltotilausRepository;
import fi.kurvinen.korjaamo.repository.VaraosaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Varaosien CRUD-logiikka (vaatimus 4). */
@Service
public class VaraosaService {

    private final VaraosaRepository repository;
    private final HuoltotilausRepository huoltotilausRepository;
    private final MuutosJulkaisija julkaisija;

    public VaraosaService(VaraosaRepository repository, HuoltotilausRepository huoltotilausRepository, MuutosJulkaisija julkaisija) {
        this.repository = repository;
        this.huoltotilausRepository = huoltotilausRepository;
        this.julkaisija = julkaisija;
    }

    @Transactional(readOnly = true)
    public List<Varaosa> kaikki() {
        return repository.findAllByOrderByNimiAsc();
    }

    @Transactional
    public Varaosa tallenna(Varaosa varaosa) {
        varaosa.setTuotenumero(varaosa.getTuotenumero().trim().toUpperCase(Locale.ROOT));
        boolean varattu = varaosa.isNew()
                ? repository.existsByTuotenumero(varaosa.getTuotenumero())
                : repository.existsByTuotenumeroAndIdNot(varaosa.getTuotenumero(), varaosa.getId());
        if (varattu) {
            throw new LiiketoimintaVirhe("Tuotenumero " + varaosa.getTuotenumero() + " on jo käytössä");
        }
        boolean uusi = varaosa.isNew();
        Varaosa tallennettu = repository.save(varaosa);
        julkaisija.julkaise("Varaosa", uusi ? "lisätty" : "muokattu", tallennettu.toString());
        return tallennettu;
    }

    @Transactional
    public void poista(Varaosa varaosa) {
        if (huoltotilausRepository.kaytetaankoVaraosaa(varaosa)) {
            throw new LiiketoimintaVirhe("Varaosaa on käytetty huoltotilauksissa, joten sitä ei voi poistaa.");
        }
        repository.deleteById(varaosa.getId());
        julkaisija.julkaise("Varaosa", "poistettu", varaosa.toString());
    }
}
