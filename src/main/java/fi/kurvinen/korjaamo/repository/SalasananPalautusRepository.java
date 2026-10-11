package fi.kurvinen.korjaamo.repository;

import fi.kurvinen.korjaamo.domain.Kayttaja;
import fi.kurvinen.korjaamo.domain.SalasananPalautus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/** Salasanan palautustokenit (vaatimus 27). */
public interface SalasananPalautusRepository extends JpaRepository<SalasananPalautus, Long> {

    Optional<SalasananPalautus> findByTokenHash(String tokenHash);

    /** Uusi pyyntö mitätöi aiemmat, jotta vain viimeisin sähköpostiin lähetetty linkki toimii. */
    @Modifying
    @Query("update SalasananPalautus s set s.kaytetty = true where s.kayttaja = :kayttaja and s.kaytetty = false")
    int mitatoiAiemmat(Kayttaja kayttaja);
}
