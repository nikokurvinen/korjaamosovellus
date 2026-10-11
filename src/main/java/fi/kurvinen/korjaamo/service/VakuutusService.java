package fi.kurvinen.korjaamo.service;

import fi.kurvinen.korjaamo.domain.Vakuutus;
import fi.kurvinen.korjaamo.repository.VakuutusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Vakuutusten CRUD-logiikka (vaatimus 2). */
@Service
public class VakuutusService {

    private final VakuutusRepository repository;
    private final MuutosJulkaisija julkaisija;

    public VakuutusService(VakuutusRepository repository, MuutosJulkaisija julkaisija) {
        this.repository = repository;
        this.julkaisija = julkaisija;
    }

    @Transactional(readOnly = true)
    public List<Vakuutus> kaikki() {
        return repository.findAllByOrderByPaattyyAsc();
    }

    @Transactional
    public Vakuutus tallenna(Vakuutus vakuutus) {
        vakuutus.setVakuutusnumero(vakuutus.getVakuutusnumero().trim().toUpperCase(Locale.ROOT));
        boolean varattu = vakuutus.isNew()
                ? repository.existsByVakuutusnumero(vakuutus.getVakuutusnumero())
                : repository.existsByVakuutusnumeroAndIdNot(vakuutus.getVakuutusnumero(), vakuutus.getId());
        if (varattu) {
            throw new LiiketoimintaVirhe("Vakuutusnumero on jo käytössä");
        }
        // 1:1-ehto tarkistetaan myös tässä, jotta käyttäjä saa selkeän viestin eikä unique-rikkomusta.
        repository.findByAjoneuvo(vakuutus.getAjoneuvo())
                .filter(v -> !v.getId().equals(vakuutus.getId()))
                .ifPresent(v -> {
                    throw new LiiketoimintaVirhe("Ajoneuvolla on jo vakuutus (" + v + ")");
                });
        boolean uusi = vakuutus.isNew();
        Vakuutus tallennettu = repository.save(vakuutus);
        julkaisija.julkaise("Vakuutus", uusi ? "lisätty" : "muokattu", tallennettu.toString());
        return tallennettu;
    }

    @Transactional
    public void poista(Vakuutus vakuutus) {
        repository.deleteById(vakuutus.getId());
        julkaisija.julkaise("Vakuutus", "poistettu", vakuutus.toString());
    }
}
