package fi.kurvinen.korjaamo.domain;

/**
 * Huoltotilauksen elinkaari. Järjestys vastaa työjononäkymän sarakkeita vasemmalta oikealle.
 * CSS-väri pidetään enumissa, jotta tila-badget ja kanban-sarakkeet käyttävät samaa väriä.
 */
public enum HuoltoTila {
    JONOSSA("Jonossa", "var(--korjaamo-tila-jonossa)"),
    TYON_ALLA("Työn alla", "var(--korjaamo-tila-tyon-alla)"),
    VALMIS("Valmis", "var(--korjaamo-tila-valmis)"),
    LUOVUTETTU("Luovutettu", "var(--korjaamo-tila-luovutettu)");

    private final String nimi;
    private final String vari;

    HuoltoTila(String nimi, String vari) {
        this.nimi = nimi;
        this.vari = vari;
    }

    public String getNimi() { return nimi; }
    public String getVari() { return vari; }

    /** Seuraava tila työjonossa, tai null jos tilaus on jo luovutettu. */
    public HuoltoTila seuraava() {
        int i = ordinal() + 1;
        return i < values().length ? values()[i] : null;
    }
}
