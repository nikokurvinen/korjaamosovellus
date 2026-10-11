package fi.kurvinen.korjaamo.repository;

import fi.kurvinen.korjaamo.domain.Huoltotilaus;

import java.util.List;

/** Dynaamisen Criteria API -haun rajapinta (Suodattaminen, kohdat 1–5). */
public interface HuoltotilausRepositoryCustom {

    List<Huoltotilaus> hae(HuoltoHakuehdot ehdot, int offset, int limit);

    long laske(HuoltoHakuehdot ehdot);
}
