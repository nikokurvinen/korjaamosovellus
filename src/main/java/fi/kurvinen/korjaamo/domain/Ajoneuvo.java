package fi.kurvinen.korjaamo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.envers.Audited;

/**
 * Ajoneuvo – sovelluksen ensimmäinen pääentiteetti (vaatimus 1).
 * <p>
 * Relaatiot:
 * <ul>
 *   <li>1:1 {@link Vakuutus} (vaatimus 2). Tämä on käänteinen puoli; vakuutus omistaa vierasavaimen.</li>
 *   <li>N:1 {@link Asiakas} (vaatimus 3). Ajoneuvotaulu sisältää vierasavaimen {@code omistaja_id}.</li>
 * </ul>
 * Vaatimus 5: rekisterinumero, merkki, malli, vuosimalli, kilometrit ja VIN validoidaan.
 */
@Entity
@Audited
public class Ajoneuvo extends AbstractAuditableEntity {

    /** Suomalainen rekisterikilpi: 1–3 kirjainta, viiva ja 1–3 numeroa (esim. ABC-123). */
    @NotBlank(message = "Rekisterinumero on pakollinen")
    @Pattern(regexp = "^[A-ZÅÄÖ]{1,3}-[0-9]{1,3}$", message = "Muoto ABC-123 (isot kirjaimet)")
    @Column(unique = true, length = 7)
    private String rekisterinumero;

    @NotBlank(message = "Merkki on pakollinen")
    @Size(max = 40, message = "Merkki saa olla enintään 40 merkkiä")
    private String merkki;

    @NotBlank(message = "Malli on pakollinen")
    @Size(max = 60, message = "Malli saa olla enintään 60 merkkiä")
    private String malli;

    @NotNull(message = "Vuosimalli on pakollinen")
    @Min(value = 1950, message = "Vuosimalli vähintään 1950")
    @Max(value = 2027, message = "Vuosimalli enintään 2027")
    private Integer vuosimalli;

    @NotNull(message = "Kilometrit ovat pakollinen tieto")
    @PositiveOrZero(message = "Kilometrit eivät voi olla negatiivisia")
    private Integer kilometrit;

    /** VIN-tunnuksessa ei käytetä kirjaimia I, O ja Q, koska ne sekoittuvat numeroihin 1 ja 0. */
    @NotBlank(message = "VIN on pakollinen")
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "VIN on 17 merkkiä (ei I, O, Q)")
    @Column(length = 17)
    private String vin;

    @NotNull(message = "Valitse omistaja")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "omistaja_id", nullable = false)
    private Asiakas omistaja;

    @OneToOne(mappedBy = "ajoneuvo", fetch = FetchType.LAZY)
    private Vakuutus vakuutus;

    public String getKuvaus() { return rekisterinumero + " – " + merkki + " " + malli; }

    public String getRekisterinumero() { return rekisterinumero; }
    public void setRekisterinumero(String rekisterinumero) { this.rekisterinumero = rekisterinumero; }
    public String getMerkki() { return merkki; }
    public void setMerkki(String merkki) { this.merkki = merkki; }
    public String getMalli() { return malli; }
    public void setMalli(String malli) { this.malli = malli; }
    public Integer getVuosimalli() { return vuosimalli; }
    public void setVuosimalli(Integer vuosimalli) { this.vuosimalli = vuosimalli; }
    public Integer getKilometrit() { return kilometrit; }
    public void setKilometrit(Integer kilometrit) { this.kilometrit = kilometrit; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public Asiakas getOmistaja() { return omistaja; }
    public void setOmistaja(Asiakas omistaja) { this.omistaja = omistaja; }
    public Vakuutus getVakuutus() { return vakuutus; }
    public void setVakuutus(Vakuutus vakuutus) { this.vakuutus = vakuutus; }

    @Override
    public String toString() { return getKuvaus(); }
}
