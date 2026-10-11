package fi.kurvinen.korjaamo.repository;

import fi.kurvinen.korjaamo.domain.Ajoneuvo;
import fi.kurvinen.korjaamo.domain.Asiakas;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Ajoneuvojen repository (vaatimus 1). */
public interface AjoneuvoRepository extends JpaRepository<Ajoneuvo, Long> {

    /*
     * Gridissä näytetään sekä omistaja (N:1) että vakuutus (1:1). Käänteistä OneToOne-relaatiota
     * Hibernate ei pysty lataamaan laiskasti ilman bytecode-enhancementia, joten se haetaan heti
     * samalla kyselyllä – muuten jokainen rivi tekisi erillisen vakuutushaun.
     */
    @EntityGraph(attributePaths = {"omistaja", "vakuutus"})
    List<Ajoneuvo> findAllByOrderByRekisterinumeroAsc();

    @EntityGraph(attributePaths = {"omistaja", "vakuutus"})
    @Query("select a from Ajoneuvo a where a.vakuutus is null order by a.rekisterinumero")
    List<Ajoneuvo> findIlmanVakuutusta();

    @EntityGraph(attributePaths = "omistaja")
    Optional<Ajoneuvo> findWithOmistajaById(Long id);

    boolean existsByRekisterinumeroAndIdNot(String rekisterinumero, Long id);

    boolean existsByRekisterinumero(String rekisterinumero);

    long countByOmistaja(Asiakas omistaja);
}
