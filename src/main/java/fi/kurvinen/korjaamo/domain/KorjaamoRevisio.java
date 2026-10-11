package fi.kurvinen.korjaamo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.time.Instant;

/**
 * Oma Envers-revisioentiteetti (vaatimus 31).
 * <p>
 * Oletusrevisio tallentaa vain numeron ja aikaleiman. Tähän lisätään kenttä {@code kayttaja}, jonka
 * {@link KorjaamoRevisioListener} täyttää jokaiseen revisioon. Näin historianäkymä voi kertoa,
 * kuka muutoksen teki, myös poistoista, joista entiteetin omat auditointikentät eivät kerro mitään.
 */
@Entity
@Table(name = "revisio")
@RevisionEntity(KorjaamoRevisioListener.class)
public class KorjaamoRevisio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @RevisionNumber
    private int id;

    @RevisionTimestamp
    private long aikaleima;

    @Column(length = 50)
    private String kayttaja;

    public int getId() { return id; }
    public long getAikaleima() { return aikaleima; }
    public Instant getAika() { return Instant.ofEpochMilli(aikaleima); }
    public String getKayttaja() { return kayttaja; }
    public void setKayttaja(String kayttaja) { this.kayttaja = kayttaja; }
}
