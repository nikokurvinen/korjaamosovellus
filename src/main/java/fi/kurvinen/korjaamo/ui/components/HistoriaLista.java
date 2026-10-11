package fi.kurvinen.korjaamo.ui.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import fi.kurvinen.korjaamo.service.HistoriaService.HistoriaMerkinta;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Muutoshistoria pystysuuntaisena aikajanana (vaatimus 32). Ulkoasu tulee teeman
 * .aikajana-luokista; pisteen väri kertoo muutostyypin (lisäys / muutos / poisto).
 */
public class HistoriaLista extends Div {

    private static final DateTimeFormatter AIKA = DateTimeFormatter.ofPattern("d.M.yyyy 'klo' HH:mm:ss");

    public HistoriaLista() {
        addClassName("aikajana");
    }

    public void nayta(List<HistoriaMerkinta> merkinnat, boolean naytaKohde) {
        removeAll();
        if (merkinnat.isEmpty()) {
            add(new Span("Ei muutoshistoriaa."));
            return;
        }
        for (HistoriaMerkinta m : merkinnat) {
            Span otsikko = new Span(m.toiminto().getNimi() + (naytaKohde ? " · " + m.kohdetyyppi() + ": " + m.kohde() : ""));
            otsikko.getStyle().set("font-weight", "600").set("display", "block");
            Span tiedot = new Span(AIKA.format(m.aika()) + " · " + m.kayttaja() + " · revisio #" + m.revisio());
            tiedot.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("color", "var(--lumo-secondary-text-color)")
                    .set("display", "block");
            Div kohta = new Div(otsikko, tiedot);
            if (!m.muuttuneetKentat().isEmpty()) {
                Span kentat = new Span("Muuttuneet kentät: " + String.join(", ", m.muuttuneetKentat()));
                kentat.getStyle().set("font-size", "var(--lumo-font-size-s)").set("display", "block");
                kohta.add(kentat);
            }
            kohta.addClassNames("aikajana-kohta", m.toiminto().name().toLowerCase(Locale.ROOT));
            add(kohta);
        }
    }
}
