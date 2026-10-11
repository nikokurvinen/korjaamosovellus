package fi.kurvinen.korjaamo.service;

import java.time.LocalDateTime;

/**
 * Tieto tallennetusta muutoksesta, joka välitetään Pushilla kaikille avoimille näkymille (vaatimus 22).
 *
 * @param kohde    entiteetin tyyppi käyttäjälle näytettävänä nimenä (esim. "Huoltotilaus")
 * @param toiminto mitä tapahtui (lisätty / muokattu / poistettu / tila vaihtui)
 * @param kuvaus   lyhyt kuvaus kohteesta
 * @param tekija   muutoksen tekijän käyttäjätunnus
 * @param aika     muutoksen ajankohta
 * @param kohdeId  muuttuneen entiteetin id (null, jos muutos koski useaa riviä)
 */
public record MuutosTapahtuma(String kohde, String toiminto, String kuvaus, String tekija, LocalDateTime aika, Long kohdeId) {
}
