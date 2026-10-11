package fi.kurvinen.korjaamo.repository;

import fi.kurvinen.korjaamo.domain.Ajoneuvo;
import fi.kurvinen.korjaamo.domain.HuoltoTila;
import fi.kurvinen.korjaamo.domain.Huoltotilaus;
import fi.kurvinen.korjaamo.domain.Varaosa;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Huoltotilausten repository. Perii Spring Datan CRUD-metodien lisäksi
 * {@link HuoltotilausRepositoryCustom}-rajapinnan, jonka Criteria API -toteutus on luokassa
 * {@link HuoltotilausRepositoryImpl} (Spring Data yhdistää ne nimen "Impl"-päätteen perusteella).
 */
public interface HuoltotilausRepository extends JpaRepository<Huoltotilaus, Long>, HuoltotilausRepositoryCustom {

    @EntityGraph(attributePaths = {"ajoneuvo", "ajoneuvo.omistaja", "varaosat"})
    List<Huoltotilaus> findAllByOrderByVastaanotettuDesc();

    @EntityGraph(attributePaths = {"ajoneuvo", "ajoneuvo.omistaja"})
    List<Huoltotilaus> findByTilaOrderByVastaanotettuAsc(HuoltoTila tila);

    @EntityGraph(attributePaths = {"ajoneuvo", "ajoneuvo.omistaja", "varaosat", "liitteet"})
    Optional<Huoltotilaus> findWithKaikkiById(Long id);

    long countByTila(HuoltoTila tila);

    boolean existsByAjoneuvo(Ajoneuvo ajoneuvo);

    @Query("select count(h) > 0 from Huoltotilaus h join h.varaosat v where v = :varaosa")
    boolean kaytetaankoVaraosaa(Varaosa varaosa);

    List<Huoltotilaus> findByVastaanotettuGreaterThanEqual(LocalDate alkaen);
}
