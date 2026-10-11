package fi.kurvinen.korjaamo.domain;

import fi.kurvinen.korjaamo.security.SecurityAuditorAware;
import org.hibernate.envers.RevisionListener;

/**
 * Täyttää revisioon muutoksen tekijän. Envers luo listenerin itse (ei Spring-bean), joten tekijä
 * haetaan staattisella apumetodilla suoraan Spring Securityn kontekstista.
 */
public class KorjaamoRevisioListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        ((KorjaamoRevisio) revisionEntity).setKayttaja(SecurityAuditorAware.nykyinenKayttaja());
    }
}
