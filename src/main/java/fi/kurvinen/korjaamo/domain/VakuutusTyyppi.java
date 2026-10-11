package fi.kurvinen.korjaamo.domain;

/** Vakuutuksen laajuus. Näyttönimi pidetään enumissa, jotta se on sama kaikissa näkymissä. */
public enum VakuutusTyyppi {
    LIIKENNE("Liikennevakuutus"),
    OSAKASKO("Osakasko"),
    KASKO("Täyskasko");

    private final String nimi;

    VakuutusTyyppi(String nimi) { this.nimi = nimi; }

    public String getNimi() { return nimi; }
}
