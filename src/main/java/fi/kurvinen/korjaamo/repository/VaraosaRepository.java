package fi.kurvinen.korjaamo.repository;

import fi.kurvinen.korjaamo.domain.Varaosa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Varaosien repository (vaatimus 4, M:N huoltotilauksiin). */
public interface VaraosaRepository extends JpaRepository<Varaosa, Long> {

    List<Varaosa> findAllByOrderByNimiAsc();

    Optional<Varaosa> findByTuotenumero(String tuotenumero);

    boolean existsByTuotenumeroAndIdNot(String tuotenumero, Long id);

    boolean existsByTuotenumero(String tuotenumero);
}
