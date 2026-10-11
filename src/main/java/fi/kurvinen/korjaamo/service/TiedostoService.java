package fi.kurvinen.korjaamo.service;

import fi.kurvinen.korjaamo.domain.Huoltotilaus;
import fi.kurvinen.korjaamo.domain.Liite;
import fi.kurvinen.korjaamo.repository.HuoltotilausRepository;
import fi.kurvinen.korjaamo.repository.LiiteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Huoltotilausten liitetiedostojen tallennus levylle (vaatimus 28).
 * <p>
 * Levyllä tiedoston nimi on UUID, ei käyttäjän antama nimi: näin nimi ei voi sisältää polkuja
 * ({@code ../}) eikä kahden käyttäjän samanniminen tiedosto ylikirjoita toista. Alkuperäinen nimi
 * säilyy tietokannassa ja palautetaan latauksen yhteydessä.
 */
@Service
public class TiedostoService {

    public static final Set<String> SALLITUT_TYYPIT = Set.of("image/png", "image/jpeg", "image/webp", "application/pdf", "text/plain");

    private final Path hakemisto;
    private final LiiteRepository liiteRepository;
    private final HuoltotilausRepository huoltotilausRepository;

    public TiedostoService(@Value("${app.upload-dir}") String hakemisto, LiiteRepository liiteRepository,
                           HuoltotilausRepository huoltotilausRepository) throws IOException {
        this.hakemisto = Path.of(hakemisto).toAbsolutePath().normalize();
        Files.createDirectories(this.hakemisto);
        this.liiteRepository = liiteRepository;
        this.huoltotilausRepository = huoltotilausRepository;
    }

    @Transactional
    public Liite tallenna(Long huoltotilausId, String tiedostonimi, String tyyppi, byte[] sisalto) {
        if (!SALLITUT_TYYPIT.contains(tyyppi)) {
            throw new LiiketoimintaVirhe("Tiedostotyyppiä " + tyyppi + " ei sallita");
        }
        if (sisalto.length == 0 || sisalto.length > Liite.MAX_KOKO) {
            throw new LiiketoimintaVirhe("Tiedoston koko on oltava 1 tavu – 10 Mt");
        }
        Huoltotilaus tilaus = huoltotilausRepository.findById(huoltotilausId).orElseThrow();
        String tallennusnimi = UUID.randomUUID().toString();
        Path kohde = polku(tallennusnimi);
        try {
            Files.write(kohde, sisalto);
        } catch (IOException e) {
            throw new UncheckedIOException("Tiedoston tallennus epäonnistui", e);
        }
        // Jos metatietojen tallennus epäonnistuu, levylle ei saa jäädä orpoa tiedostoa.
        poistaJosRollback(kohde);

        Liite liite = new Liite();
        // Pelkkä tiedoston nimi ilman mahdollista selaimen lähettämää polkua (vanhat IE-versiot).
        liite.setTiedostonimi(Path.of(tiedostonimi).getFileName().toString());
        liite.setSisaltotyyppi(tyyppi);
        liite.setKoko(sisalto.length);
        liite.setTallennusnimi(tallennusnimi);
        liite.setHuoltotilaus(tilaus);
        return liiteRepository.save(liite);
    }

    @Transactional(readOnly = true)
    public List<Liite> liitteet(Huoltotilaus tilaus) {
        return liiteRepository.findByHuoltotilausOrderByLisattyDesc(tilaus);
    }

    public InputStream avaa(Liite liite) throws IOException {
        return Files.newInputStream(polku(liite.getTallennusnimi()));
    }

    @Transactional
    public void poista(Liite liite) {
        liiteRepository.deleteById(liite.getId());
        try {
            Files.deleteIfExists(polku(liite.getTallennusnimi()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Varmistaa vielä, että ratkaistu polku pysyy upload-hakemiston sisällä. */
    private Path polku(String tallennusnimi) {
        Path p = hakemisto.resolve(tallennusnimi).normalize();
        if (!p.startsWith(hakemisto)) {
            throw new SecurityException("Virheellinen tiedostopolku");
        }
        return p;
    }

    private static void poistaJosRollback(Path tiedosto) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        try {
                            Files.deleteIfExists(tiedosto);
                        } catch (IOException ignored) {
                            // Siivous on parhaan yrityksen varassa.
                        }
                    }
                }
            });
        }
    }
}
