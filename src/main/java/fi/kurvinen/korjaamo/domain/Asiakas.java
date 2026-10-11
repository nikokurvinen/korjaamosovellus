package fi.kurvinen.korjaamo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.envers.Audited;

import java.util.ArrayList;
import java.util.List;

/**
 * Korjaamon asiakas eli ajoneuvon omistaja.
 * <p>
 * Vaatimus 3 (1:N): yhdellä asiakkaalla voi olla useita ajoneuvoja. Tämä on relaation käänteinen
 * puoli ({@code mappedBy}); vierasavain {@code omistaja_id} on ajoneuvotaulussa, koska "monta"-puoli
 * omistaa 1:N-relaation relaatiotietokannassa aina.
 * <p>
 * Vaatimus 5: validoitavat kentät etunimi, sukunimi, sähköposti, puhelin, postinumero ja kaupunki.
 */
@Entity
@Audited
public class Asiakas extends AbstractAuditableEntity {

    @NotBlank(message = "Etunimi on pakollinen")
    @Size(max = 50, message = "Etunimi saa olla enintään 50 merkkiä")
    private String etunimi;

    @NotBlank(message = "Sukunimi on pakollinen")
    @Size(max = 50, message = "Sukunimi saa olla enintään 50 merkkiä")
    private String sukunimi;

    @NotBlank(message = "Sähköposti on pakollinen")
    @Email(message = "Sähköpostiosoite ei ole kelvollinen")
    @Column(unique = true, length = 120)
    private String email;

    @NotBlank(message = "Puhelinnumero on pakollinen")
    @Pattern(regexp = "^\\+?[0-9 ]{7,15}$", message = "Puhelinnumero: 7–15 numeroa, sallittu + ja välilyönnit")
    private String puhelin;

    @Size(max = 100, message = "Katuosoite saa olla enintään 100 merkkiä")
    private String katuosoite;

    @Pattern(regexp = "^[0-9]{5}$", message = "Postinumerossa on 5 numeroa")
    private String postinumero;

    @NotBlank(message = "Kaupunki on pakollinen")
    @Size(max = 50, message = "Kaupunki saa olla enintään 50 merkkiä")
    private String kaupunki;

    @OneToMany(mappedBy = "omistaja")
    @OrderBy("rekisterinumero")
    private List<Ajoneuvo> ajoneuvot = new ArrayList<>();

    public String getKokoNimi() { return etunimi + " " + sukunimi; }

    public String getEtunimi() { return etunimi; }
    public void setEtunimi(String etunimi) { this.etunimi = etunimi; }
    public String getSukunimi() { return sukunimi; }
    public void setSukunimi(String sukunimi) { this.sukunimi = sukunimi; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPuhelin() { return puhelin; }
    public void setPuhelin(String puhelin) { this.puhelin = puhelin; }
    public String getKatuosoite() { return katuosoite; }
    public void setKatuosoite(String katuosoite) { this.katuosoite = katuosoite; }
    public String getPostinumero() { return postinumero; }
    public void setPostinumero(String postinumero) { this.postinumero = postinumero; }
    public String getKaupunki() { return kaupunki; }
    public void setKaupunki(String kaupunki) { this.kaupunki = kaupunki; }
    public List<Ajoneuvo> getAjoneuvot() { return ajoneuvot; }

    @Override
    public String toString() { return getKokoNimi(); }
}
