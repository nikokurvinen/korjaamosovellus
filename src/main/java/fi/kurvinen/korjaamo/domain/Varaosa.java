package fi.kurvinen.korjaamo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Varaosa – vaatimus 4: M:N-relaatio huoltotilauksiin. Yhteen huoltoon kuluu useita osia, ja
 * samaa osaa käytetään monessa huollossa.
 * <p>
 * Tämä on relaation käänteinen puoli ({@code mappedBy}); liitostaulun {@code huoltotilaus_varaosa}
 * rivejä hallitaan {@link Huoltotilaus}-puolelta. Kokoelma on {@link Set}, koska Hibernate
 * poistaisi ja lisäisi List-tyyppisen M:N-kokoelman kaikki rivit uudelleen jokaisella muutoksella.
 * <p>
 * Vaatimus 5: tuotenumero, nimi, valmistaja, hinta ja varastosaldo validoidaan. Varaosat ovat myös
 * CSV-tuonnin ja -viennin kohde (vaatimus 29).
 */
@Entity
@Audited
public class Varaosa extends AbstractAuditableEntity {

    @NotBlank(message = "Tuotenumero on pakollinen")
    @Pattern(regexp = "^[A-Z]{2,4}-[0-9]{3,6}$", message = "Muoto: 2–4 isoa kirjainta, viiva, 3–6 numeroa (esim. JAR-1042)")
    @Column(unique = true, length = 11)
    private String tuotenumero;

    @NotBlank(message = "Nimi on pakollinen")
    @Size(max = 80, message = "Nimi saa olla enintään 80 merkkiä")
    private String nimi;

    @NotBlank(message = "Valmistaja on pakollinen")
    @Size(max = 50, message = "Valmistaja saa olla enintään 50 merkkiä")
    private String valmistaja;

    @NotNull(message = "Hinta on pakollinen")
    @Positive(message = "Hinnan on oltava positiivinen")
    @Digits(integer = 6, fraction = 2, message = "Enintään 999999,99 €")
    private BigDecimal hinta;

    @NotNull(message = "Varastosaldo on pakollinen")
    @PositiveOrZero(message = "Saldo ei voi olla negatiivinen")
    @Max(value = 9999, message = "Saldo enintään 9999")
    private Integer varastosaldo;

    @ManyToMany(mappedBy = "varaosat")
    private Set<Huoltotilaus> huoltotilaukset = new HashSet<>();

    public String getTuotenumero() { return tuotenumero; }
    public void setTuotenumero(String tuotenumero) { this.tuotenumero = tuotenumero; }
    public String getNimi() { return nimi; }
    public void setNimi(String nimi) { this.nimi = nimi; }
    public String getValmistaja() { return valmistaja; }
    public void setValmistaja(String valmistaja) { this.valmistaja = valmistaja; }
    public BigDecimal getHinta() { return hinta; }
    public void setHinta(BigDecimal hinta) { this.hinta = hinta; }
    public Integer getVarastosaldo() { return varastosaldo; }
    public void setVarastosaldo(Integer varastosaldo) { this.varastosaldo = varastosaldo; }
    public Set<Huoltotilaus> getHuoltotilaukset() { return huoltotilaukset; }

    @Override
    public String toString() { return tuotenumero + " " + nimi; }
}
