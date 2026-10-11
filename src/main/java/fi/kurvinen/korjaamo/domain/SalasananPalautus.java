package fi.kurvinen.korjaamo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/**
 * Salasanan palautuspyyntö (vaatimus 27).
 * <p>
 * Tietokantaan tallennetaan vain tokenin SHA-256-tiiviste. Jos tietokanta vuotaisi, voimassa
 * olevilla palautuslinkeillä ei voisi kaapata tilejä. Token on kertakäyttöinen ({@code kaytetty})
 * ja vanhenee 30 minuutissa, mikä rajaa sähköpostiin jääneen linkin hyödyntämisen aikaikkunaa.
 */
@Entity
public class SalasananPalautus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kayttaja_id")
    private Kayttaja kayttaja;

    @Column(nullable = false)
    private Instant vanhenee;

    private boolean kaytetty;

    protected SalasananPalautus() {
    }

    public SalasananPalautus(String tokenHash, Kayttaja kayttaja, Instant vanhenee) {
        this.tokenHash = tokenHash;
        this.kayttaja = kayttaja;
        this.vanhenee = vanhenee;
    }

    public boolean isKelvollinen(Instant nyt) {
        return !kaytetty && nyt.isBefore(vanhenee);
    }

    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public Kayttaja getKayttaja() { return kayttaja; }
    public Instant getVanhenee() { return vanhenee; }
    public boolean isKaytetty() { return kaytetty; }
    public void merkitseKaytetyksi() { this.kaytetty = true; }
}
