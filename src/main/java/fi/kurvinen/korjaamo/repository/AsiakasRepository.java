package fi.kurvinen.korjaamo.repository;

import fi.kurvinen.korjaamo.domain.Asiakas;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Asiakkaiden repository (vaatimus 1–3). */
public interface AsiakasRepository extends JpaRepository<Asiakas, Long> {

    /**
     * Asiakasgrid näyttää jokaisen asiakkaan ajoneuvot (1:N). EntityGraph hakee ne samalla
     * kyselyllä; muuten jokainen gridin rivi laukaisisi oman SELECTin (N+1-ongelma).
     */
    @EntityGraph(attributePaths = "ajoneuvot")
    List<Asiakas> findAllByOrderBySukunimiAscEtunimiAsc();

    List<Asiakas> findAllByOrderBySukunimiAsc();

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByEmailIgnoreCase(String email);
}
