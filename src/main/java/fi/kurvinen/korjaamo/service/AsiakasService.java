package fi.kurvinen.korjaamo.service;

import fi.kurvinen.korjaamo.domain.Asiakas;
import fi.kurvinen.korjaamo.repository.AjoneuvoRepository;
import fi.kurvinen.korjaamo.repository.AsiakasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Asiakkaiden CRUD-logiikka (vaatimus 3). */
@Service
public class AsiakasService {

    private final AsiakasRepository repository;
    private final AjoneuvoRepository ajoneuvoRepository;
    private final MuutosJulkaisija julkaisija;

    public AsiakasService(AsiakasRepository repository, AjoneuvoRepository ajoneuvoRepository, MuutosJulkaisija julkaisija) {
        this.repository = repository;
        this.ajoneuvoRepository = ajoneuvoRepository;
        this.julkaisija = julkaisija;
    }

    @Transactional(readOnly = true)
    public List<Asiakas> kaikkiAjoneuvoineen() {
        return repository.findAllByOrderBySukunimiAscEtunimiAsc();
    }

    @Transactional(readOnly = true)
    public List<Asiakas> kaikki() {
        return repository.findAllByOrderBySukunimiAsc();
    }

    @Transactional
    public Asiakas tallenna(Asiakas asiakas) {
        asiakas.setEmail(asiakas.getEmail().trim().toLowerCase(Locale.ROOT));
        boolean varattu = asiakas.isNew()
                ? repository.existsByEmailIgnoreCase(asiakas.getEmail())
                : repository.existsByEmailIgnoreCaseAndIdNot(asiakas.getEmail(), asiakas.getId());
        if (varattu) {
            throw new LiiketoimintaVirhe("Sähköposti " + asiakas.getEmail() + " on jo toisella asiakkaalla");
        }
        boolean uusi = asiakas.isNew();
        Asiakas tallennettu = repository.save(asiakas);
        julkaisija.julkaise("Asiakas", uusi ? "lisätty" : "muokattu", tallennettu.getKokoNimi());
        return tallennettu;
    }

    @Transactional
    public void poista(Asiakas asiakas) {
        // Ajoneuvoilla on pakollinen omistaja, joten asiakasta ei voi poistaa ennen niitä.
        // Selkeä viesti on parempi kuin tietokannan vierasavainvirhe.
        if (ajoneuvoRepository.countByOmistaja(asiakas) > 0) {
            throw new LiiketoimintaVirhe("Asiakkaalla on ajoneuvoja. Poista tai siirrä ne ensin.");
        }
        repository.deleteById(asiakas.getId());
        julkaisija.julkaise("Asiakas", "poistettu", asiakas.getKokoNimi());
    }
}
