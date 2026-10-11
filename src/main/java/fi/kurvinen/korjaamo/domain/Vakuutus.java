package fi.kurvinen.korjaamo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ajoneuvon vakuutus – vaatimus 2: 1:1-relaatio ensimmäiseen entiteettiin ({@link Ajoneuvo}).
 * <p>
 * Vakuutus on relaation omistava puoli: vierasavain {@code ajoneuvo_id} on tässä taulussa ja sille
 * on asetettu {@code unique = true}. Ilman unique-rajoitetta tietokanta sallisisi saman ajoneuvon
 * kaksi vakuutusta, jolloin relaatio olisi käytännössä N:1 eikä 1:1.
 * <p>
 * Vaatimus 5: yhtiö, vakuutusnumero, tyyppi, voimassaolopäivät ja omavastuu validoidaan.
 */
@Entity
@Audited
public class Vakuutus extends AbstractAuditableEntity {

    @NotBlank(message = "Vakuutusyhtiö on pakollinen")
    @Size(max = 60, message = "Yhtiön nimi saa olla enintään 60 merkkiä")
    private String yhtio;

    @NotBlank(message = "Vakuutusnumero on pakollinen")
    @Pattern(regexp = "^[A-Z]{2}[0-9]{6,10}$", message = "Muoto: kaksi kirjainta + 6–10 numeroa (esim. LT12345678)")
    @Column(unique = true, length = 12)
    private String vakuutusnumero;

    @NotNull(message = "Valitse vakuutustyyppi")
    @Enumerated(EnumType.STRING)
    private VakuutusTyyppi tyyppi;

    @NotNull(message = "Alkamispäivä on pakollinen")
    private LocalDate alkaen;

    @NotNull(message = "Päättymispäivä on pakollinen")
    private LocalDate paattyy;

    @NotNull(message = "Omavastuu on pakollinen")
    @PositiveOrZero(message = "Omavastuu ei voi olla negatiivinen")
    @Digits(integer = 5, fraction = 2, message = "Enintään 99999,99 €")
    private BigDecimal omavastuu;

    @NotNull(message = "Valitse ajoneuvo")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ajoneuvo_id", nullable = false, unique = true)
    private Ajoneuvo ajoneuvo;

    /**
     * Ristiinvalidointi kahden kentän välillä. Bean Validation kutsuu is-alkuista getteriä, ja
     * BeanValidationBinder näyttää viestin lomakkeen yleisvirheenä.
     */
    @AssertTrue(message = "Päättymispäivän on oltava alkamispäivän jälkeen")
    public boolean isVoimassaoloJarkeva() {
        return alkaen == null || paattyy == null || paattyy.isAfter(alkaen);
    }

    public boolean isVoimassa() {
        LocalDate nyt = LocalDate.now();
        return alkaen != null && paattyy != null && !nyt.isBefore(alkaen) && !nyt.isAfter(paattyy);
    }

    public String getYhtio() { return yhtio; }
    public void setYhtio(String yhtio) { this.yhtio = yhtio; }
    public String getVakuutusnumero() { return vakuutusnumero; }
    public void setVakuutusnumero(String vakuutusnumero) { this.vakuutusnumero = vakuutusnumero; }
    public VakuutusTyyppi getTyyppi() { return tyyppi; }
    public void setTyyppi(VakuutusTyyppi tyyppi) { this.tyyppi = tyyppi; }
    public LocalDate getAlkaen() { return alkaen; }
    public void setAlkaen(LocalDate alkaen) { this.alkaen = alkaen; }
    public LocalDate getPaattyy() { return paattyy; }
    public void setPaattyy(LocalDate paattyy) { this.paattyy = paattyy; }
    public BigDecimal getOmavastuu() { return omavastuu; }
    public void setOmavastuu(BigDecimal omavastuu) { this.omavastuu = omavastuu; }
    public Ajoneuvo getAjoneuvo() { return ajoneuvo; }
    public void setAjoneuvo(Ajoneuvo ajoneuvo) { this.ajoneuvo = ajoneuvo; }

    @Override
    public String toString() { return yhtio + " " + vakuutusnumero; }
}
