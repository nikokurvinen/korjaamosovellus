package fi.kurvinen.korjaamo.service;

/** Julkaistaan, kun uusi käyttäjä on rekisteröitynyt (vaatimus 26). */
public record KayttajaRekisteroitynyt(String kayttajatunnus, String nimi, String email) {
}
