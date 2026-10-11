package fi.kurvinen.korjaamo.repository;

import fi.kurvinen.korjaamo.domain.Ajoneuvo;
import fi.kurvinen.korjaamo.domain.Vakuutus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Vakuutusten repository (vaatimus 2, 1:1 ajoneuvoon). */
public interface VakuutusRepository extends JpaRepository<Vakuutus, Long> {

    @EntityGraph(attributePaths = {"ajoneuvo", "ajoneuvo.omistaja"})
    List<Vakuutus> findAllByOrderByPaattyyAsc();

    Optional<Vakuutus> findByAjoneuvo(Ajoneuvo ajoneuvo);

    boolean existsByVakuutusnumeroAndIdNot(String vakuutusnumero, Long id);

    boolean existsByVakuutusnumero(String vakuutusnumero);
}
