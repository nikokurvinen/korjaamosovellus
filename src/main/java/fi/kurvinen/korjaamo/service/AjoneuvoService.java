package fi.kurvinen.korjaamo.service;

import fi.kurvinen.korjaamo.domain.Ajoneuvo;
import fi.kurvinen.korjaamo.repository.AjoneuvoRepository;
import fi.kurvinen.korjaamo.repository.HuoltotilausRepository;
import fi.kurvinen.korjaamo.repository.VakuutusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Ajoneuvojen CRUD-logiikka (vaatimus 1). */
@Service
public class AjoneuvoService {

    private final AjoneuvoRepository repository;
    private final VakuutusRepository vakuutusRepository;
    private final HuoltotilausRepository huoltotilausRepository;
    private final MuutosJulkaisija julkaisija;

    public AjoneuvoService(AjoneuvoRepository repository, VakuutusRepository vakuutusRepository,
                           HuoltotilausRepository huoltotilausRepository, MuutosJulkaisija julkaisija) {
        this.repository = repository;
        this.vakuutusRepository = vakuutusRepository;
        this.huoltotilausRepository = huoltotilausRepository;
        this.julkaisija = julkaisija;
    }

    @Transactional(readOnly = true)
    public List<Ajoneuvo> kaikki() {
        return repository.findAllByOrderByRekisterinumeroAsc();
    }

    @Transactional(readOnly = true)
    public List<Ajoneuvo> ilmanVakuutusta() {
        return repository.findIlmanVakuutusta();
    }

    @Transactional
    public Ajoneuvo tallenna(Ajoneuvo ajoneuvo) {
        ajoneuvo.setRekisterinumero(ajoneuvo.getRekisterinumero().trim().toUpperCase(Locale.ROOT));
        ajoneuvo.setVin(ajoneuvo.getVin().trim().toUpperCase(Locale.ROOT));
        boolean varattu = ajoneuvo.isNew()
                ? repository.existsByRekisterinumero(ajoneuvo.getRekisterinumero())
                : repository.existsByRekisterinumeroAndIdNot(ajoneuvo.getRekisterinumero(), ajoneuvo.getId());
        if (varattu) {
            throw new LiiketoimintaVirhe("Rekisterinumero " + ajoneuvo.getRekisterinumero() + " on jo rekisteröity");
        }
        boolean uusi = ajoneuvo.isNew();
        Ajoneuvo tallennettu = repository.save(ajoneuvo);
        julkaisija.julkaise("Ajoneuvo", uusi ? "lisätty" : "muokattu", tallennettu.getKuvaus());
        return tallennettu;
    }

    /**
     * Vakuutus poistetaan ajoneuvon mukana (1:1 – vakuutus ei merkitse mitään ilman ajoneuvoa).
     * Huoltotilauksia sen sijaan ei poisteta automaattisesti, koska ne ovat kirjanpidollisesti
     * tärkeää historiaa.
     */
    @Transactional
    public void poista(Ajoneuvo ajoneuvo) {
        if (huoltotilausRepository.existsByAjoneuvo(ajoneuvo)) {
            throw new LiiketoimintaVirhe("Ajoneuvolla on huoltotilauksia, joten sitä ei voi poistaa.");
        }
        vakuutusRepository.findByAjoneuvo(ajoneuvo).ifPresent(vakuutusRepository::delete);
        repository.deleteById(ajoneuvo.getId());
        julkaisija.julkaise("Ajoneuvo", "poistettu", ajoneuvo.getKuvaus());
    }
}
