package fi.kurvinen.korjaamo.repository;

import fi.kurvinen.korjaamo.domain.Kayttaja;
import fi.kurvinen.korjaamo.domain.Rooli;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Sovelluksen käyttäjät (vaatimus 16). */
public interface KayttajaRepository extends JpaRepository<Kayttaja, Long> {

    Optional<Kayttaja> findByKayttajatunnus(String kayttajatunnus);

    Optional<Kayttaja> findByEmailIgnoreCase(String email);

    boolean existsByKayttajatunnus(String kayttajatunnus);

    boolean existsByEmailIgnoreCase(String email);

    List<Kayttaja> findByRoolitContaining(Rooli rooli);

    List<Kayttaja> findAllByOrderByKayttajatunnusAsc();
}
