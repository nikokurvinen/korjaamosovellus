package fi.kurvinen.korjaamo.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Sovelluksen oma käyttäjäentiteetti (vaatimus 16).
 * <p>
 * Salasanaa ei tallenneta missään muodossa, josta sen voisi palauttaa: kentässä
 * {@code salasanaHash} on BCrypt-tiiviste, joka sisältää satunnaisen suolan ja on laskennallisesti
 * hidas, joten vuotanutta tietokantaa ei voi murtaa sateenkaaritauluilla. Entiteetillä ei ole
 * lainkaan selkokielistä salasanakenttää.
 * <p>
 * Taulun nimi on {@code kayttaja}, koska {@code user} on varattu sana PostgreSQL:ssä.
 * Profiilikuva (vaatimus 19) tallennetaan tavuina tietokantaan: kuvat ovat pieniä (max 2 Mt), ja
 * näin ne siirtyvät tietokannan varmuuskopion mukana ilman erillistä tiedostotallennusta.
 */
@Entity
@Table(name = "kayttaja")
@EntityListeners(AuditingEntityListener.class)
public class Kayttaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Käyttäjätunnus on pakollinen")
    @Size(min = 3, max = 30, message = "Käyttäjätunnuksessa 3–30 merkkiä")
    @Pattern(regexp = "^[a-z0-9_.]+$", message = "Sallitut merkit: a–z, 0–9, _ ja .")
    @Column(unique = true, nullable = false, length = 30)
    private String kayttajatunnus;

    @NotBlank(message = "Nimi on pakollinen")
    @Size(max = 80, message = "Nimi saa olla enintään 80 merkkiä")
    private String nimi;

    @NotBlank(message = "Sähköposti on pakollinen")
    @Email(message = "Sähköpostiosoite ei ole kelvollinen")
    @Column(unique = true, nullable = false, length = 120)
    private String email;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String salasanaHash;

    /* EAGER on perusteltu: roolit tarvitaan jokaisessa kirjautumisessa ja niitä on enintään kolme. */
    @NotEmpty(message = "Käyttäjällä on oltava vähintään yksi rooli")
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "kayttaja_rooli", joinColumns = @JoinColumn(name = "kayttaja_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rooli", length = 10)
    private Set<Rooli> roolit = EnumSet.of(Rooli.USER);

    @Column(length = 3_000_000)
    private byte[] profiilikuva;

    @Column(length = 30)
    private String profiilikuvanTyyppi;

    private boolean aktiivinen = true;

    @CreatedDate
    private LocalDateTime rekisteroity;

    public Long getId() { return id; }
    public String getKayttajatunnus() { return kayttajatunnus; }
    public void setKayttajatunnus(String kayttajatunnus) { this.kayttajatunnus = kayttajatunnus; }
    public String getNimi() { return nimi; }
    public void setNimi(String nimi) { this.nimi = nimi; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSalasanaHash() { return salasanaHash; }
    public void setSalasanaHash(String salasanaHash) { this.salasanaHash = salasanaHash; }
    public Set<Rooli> getRoolit() { return roolit; }
    public void setRoolit(Set<Rooli> roolit) { this.roolit = roolit; }
    public byte[] getProfiilikuva() { return profiilikuva; }
    public void setProfiilikuva(byte[] profiilikuva) { this.profiilikuva = profiilikuva; }
    public String getProfiilikuvanTyyppi() { return profiilikuvanTyyppi; }
    public void setProfiilikuvanTyyppi(String profiilikuvanTyyppi) { this.profiilikuvanTyyppi = profiilikuvanTyyppi; }
    public boolean isAktiivinen() { return aktiivinen; }
    public void setAktiivinen(boolean aktiivinen) { this.aktiivinen = aktiivinen; }
    public LocalDateTime getRekisteroity() { return rekisteroity; }
}
