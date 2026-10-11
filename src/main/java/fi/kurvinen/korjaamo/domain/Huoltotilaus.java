package fi.kurvinen.korjaamo.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Huoltotilaus – sovelluksen keskeisin entiteetti ja Criteria-haun kohde.
 * <p>
 * Relaatiot:
 * <ul>
 *   <li>N:1 {@link Ajoneuvo}: jokainen tilaus koskee yhtä ajoneuvoa.</li>
 *   <li>M:N {@link Varaosa} (vaatimus 4): tämä on omistava puoli, joten liitostaulun
 *       {@code huoltotilaus_varaosa} rivit muodostuvat tämän kokoelman perusteella.</li>
 *   <li>1:N {@link Liite} (vaatimus 28): kuitit ja kuvat. Liitteet poistetaan tilauksen mukana
 *       ({@code orphanRemoval}), koska ne eivät merkitse mitään ilman tilausta.</li>
 * </ul>
 * Vaatimus 5: otsikko, kuvaus, tila, vastaanottopäivä, työtunnit ja hinta validoidaan, ja lisäksi
 * päivämäärien järjestys tarkistetaan ristiinvalidoinnilla.
 */
@Entity
@Audited
public class Huoltotilaus extends AbstractAuditableEntity {

    @NotBlank(message = "Otsikko on pakollinen")
    @Size(min = 3, max = 80, message = "Otsikossa 3–80 merkkiä")
    private String otsikko;

    @Size(max = 2000, message = "Kuvaus saa olla enintään 2000 merkkiä")
    @Column(length = 2000)
    private String kuvaus;

    @NotNull(message = "Valitse tila")
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private HuoltoTila tila = HuoltoTila.JONOSSA;

    @NotNull(message = "Vastaanottopäivä on pakollinen")
    private LocalDate vastaanotettu;

    private LocalDate valmistuu;

    @NotNull(message = "Työtunnit ovat pakollinen tieto")
    @DecimalMin(value = "0.0", message = "Työtunnit eivät voi olla negatiivisia")
    @DecimalMax(value = "200.0", message = "Työtunteja enintään 200")
    @Digits(integer = 3, fraction = 1, message = "Yksi desimaali riittää")
    private BigDecimal tyotunnit = BigDecimal.ZERO;

    @NotNull(message = "Hinta on pakollinen")
    @PositiveOrZero(message = "Hinta ei voi olla negatiivinen")
    @Digits(integer = 6, fraction = 2, message = "Enintään 999999,99 €")
    private BigDecimal hinta = BigDecimal.ZERO;

    @NotNull(message = "Valitse ajoneuvo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ajoneuvo_id", nullable = false)
    private Ajoneuvo ajoneuvo;

    @ManyToMany
    @JoinTable(name = "huoltotilaus_varaosa",
            joinColumns = @JoinColumn(name = "huoltotilaus_id"),
            inverseJoinColumns = @JoinColumn(name = "varaosa_id"))
    private Set<Varaosa> varaosat = new HashSet<>();

    /** Liitteet ovat tiedostoja levyllä, joten niiden historiaa ei tallenneta Enversiin. */
    @NotAudited
    @OneToMany(mappedBy = "huoltotilaus", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Liite> liitteet = new ArrayList<>();

    @AssertTrue(message = "Valmistumispäivä ei voi olla ennen vastaanottopäivää")
    public boolean isPaivatJarkevat() {
        return vastaanotettu == null || valmistuu == null || !valmistuu.isBefore(vastaanotettu);
    }

    public String getOtsikko() { return otsikko; }
    public void setOtsikko(String otsikko) { this.otsikko = otsikko; }
    public String getKuvaus() { return kuvaus; }
    public void setKuvaus(String kuvaus) { this.kuvaus = kuvaus; }
    public HuoltoTila getTila() { return tila; }
    public void setTila(HuoltoTila tila) { this.tila = tila; }
    public LocalDate getVastaanotettu() { return vastaanotettu; }
    public void setVastaanotettu(LocalDate vastaanotettu) { this.vastaanotettu = vastaanotettu; }
    public LocalDate getValmistuu() { return valmistuu; }
    public void setValmistuu(LocalDate valmistuu) { this.valmistuu = valmistuu; }
    public BigDecimal getTyotunnit() { return tyotunnit; }
    public void setTyotunnit(BigDecimal tyotunnit) { this.tyotunnit = tyotunnit; }
    public BigDecimal getHinta() { return hinta; }
    public void setHinta(BigDecimal hinta) { this.hinta = hinta; }
    public Ajoneuvo getAjoneuvo() { return ajoneuvo; }
    public void setAjoneuvo(Ajoneuvo ajoneuvo) { this.ajoneuvo = ajoneuvo; }
    public Set<Varaosa> getVaraosat() { return varaosat; }
    public void setVaraosat(Set<Varaosa> varaosat) { this.varaosat = varaosat; }
    public List<Liite> getLiitteet() { return liitteet; }

    @Override
    public String toString() { return otsikko; }
}
