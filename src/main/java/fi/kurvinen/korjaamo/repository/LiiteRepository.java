package fi.kurvinen.korjaamo.repository;

import fi.kurvinen.korjaamo.domain.Huoltotilaus;
import fi.kurvinen.korjaamo.domain.Liite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Huoltotilausten liitetiedostojen metatiedot (vaatimus 28). */
public interface LiiteRepository extends JpaRepository<Liite, Long> {

    List<Liite> findByHuoltotilausOrderByLisattyDesc(Huoltotilaus huoltotilaus);
}
