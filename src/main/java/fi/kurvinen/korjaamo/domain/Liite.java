package fi.kurvinen.korjaamo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Huoltotilaukseen liitetty tiedosto (kuitti, valokuva vauriosta, PDF-raportti) – vaatimus 28.
 * <p>
 * Itse tiedosto tallennetaan levylle; tietokantaan tallennetaan vain metatiedot. Levyllä tiedoston
 * nimi on satunnainen UUID ({@code tallennusnimi}), joten käyttäjän antama nimi ei voi koskaan
 * päätyä tiedostopolkuun (path traversal, esim. {@code ../../etc/passwd}).
 * <p>
 * Ei peri {@link AbstractAuditableEntity}:ä, koska liitettä ei muokata eikä sille tarvita
 * Envers-historiaa – luontitiedot riittävät.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Liite {

    public static final long MAX_KOKO = 10 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String tiedostonimi;

    @NotBlank
    @Pattern(regexp = "^(image/(png|jpeg|webp)|application/pdf|text/plain)$", message = "Sallitut: PNG, JPEG, WEBP, PDF, TXT")
    @Column(length = 50)
    private String sisaltotyyppi;

    @Positive
    @Max(MAX_KOKO)
    private long koko;

    @NotBlank
    @Column(length = 40, unique = true)
    private String tallennusnimi;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "huoltotilaus_id", nullable = false)
    private Huoltotilaus huoltotilaus;

    @CreatedBy
    @Column(length = 50)
    private String lisannyt;

    @CreatedDate
    private LocalDateTime lisatty;

    public Long getId() { return id; }
    public String getTiedostonimi() { return tiedostonimi; }
    public void setTiedostonimi(String tiedostonimi) { this.tiedostonimi = tiedostonimi; }
    public String getSisaltotyyppi() { return sisaltotyyppi; }
    public void setSisaltotyyppi(String sisaltotyyppi) { this.sisaltotyyppi = sisaltotyyppi; }
    public long getKoko() { return koko; }
    public void setKoko(long koko) { this.koko = koko; }
    public String getTallennusnimi() { return tallennusnimi; }
    public void setTallennusnimi(String tallennusnimi) { this.tallennusnimi = tallennusnimi; }
    public Huoltotilaus getHuoltotilaus() { return huoltotilaus; }
    public void setHuoltotilaus(Huoltotilaus huoltotilaus) { this.huoltotilaus = huoltotilaus; }
    public String getLisannyt() { return lisannyt; }
    public LocalDateTime getLisatty() { return lisatty; }
}
