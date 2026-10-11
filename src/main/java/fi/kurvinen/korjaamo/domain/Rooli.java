package fi.kurvinen.korjaamo.domain;

/**
 * Käyttäjäroolit (vaatimus 16). Spring Security odottaa rooleille ROLE_-etuliitettä, joka
 * lisätään vasta {@code UserDetails}-muunnoksessa, jotta {@code @RolesAllowed("ADMIN")} toimii.
 */
public enum Rooli {
    ADMIN, SUPER, USER
}
