package fi.kurvinen.korjaamo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.hibernate.Hibernate;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Kaikkien domain-entiteettien yhteinen yläluokka.
 * <p>
 * Vaatimus 30 (Spring Auditing): {@link AuditingEntityListener} täyttää luonti- ja muokkaustiedot
 * automaattisesti jokaisen tallennuksen yhteydessä. Tekijä saadaan
 * {@link fi.kurvinen.korjaamo.security.SecurityAuditorAware}-beanista, joten näkymien ei tarvitse
 * muistaa asettaa kenttiä itse.
 * <p>
 * {@code @Audited} yläluokassa on välttämätön: Envers ei muuten tallentaisi perittyjä kenttiä
 * historiatauluihin, vaikka aliluokka olisi auditoitu (vaatimus 31).
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Audited
public abstract class AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Optimistinen lukitus: jos kaksi mekaanikkoa muokkaa samaa tilausta yhtä aikaa, myöhempi
     * tallennus epäonnistuu sen sijaan, että se ylikirjoittaisi toisen muutokset huomaamatta.
     */
    @Version
    private Integer version;

    @CreatedBy
    @Column(updatable = false, length = 50)
    private String luonut;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime luotu;

    @LastModifiedBy
    @Column(length = 50)
    private String muokannut;

    @LastModifiedDate
    private LocalDateTime muokattu;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getVersion() { return version; }
    public String getLuonut() { return luonut; }
    public LocalDateTime getLuotu() { return luotu; }
    public String getMuokannut() { return muokannut; }
    public LocalDateTime getMuokattu() { return muokattu; }

    public boolean isNew() { return id == null; }

    /*
     * equals/hashCode perustuvat id:hen, mutta tallentamaton olio on yhtä suuri vain itsensä kanssa.
     * Hibernate.getClass purkaa lazy-proxyt, jotta proxy ja varsinainen olio tunnistetaan samaksi.
     * hashCode on luokkakohtainen vakio, jotta olio pysyy samassa HashSet-lokerossa myös sen
     * jälkeen, kun se saa id:n tallennuksessa (tärkeää M:N-relaation Set-kokoelmille).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        AbstractAuditableEntity other = (AbstractAuditableEntity) o;
        return id != null && Objects.equals(id, other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
