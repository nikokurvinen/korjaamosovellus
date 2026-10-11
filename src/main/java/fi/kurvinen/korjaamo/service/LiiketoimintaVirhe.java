package fi.kurvinen.korjaamo.service;

/**
 * Käyttäjälle näytettävä sääntövirhe (esim. "rekisterinumero on jo käytössä"). Erotetaan
 * odottamattomista poikkeuksista, jotta näkymä voi näyttää viestin sellaisenaan ilman stack tracea.
 */
public class LiiketoimintaVirhe extends RuntimeException {

    public LiiketoimintaVirhe(String viesti) {
        super(viesti);
    }
}
