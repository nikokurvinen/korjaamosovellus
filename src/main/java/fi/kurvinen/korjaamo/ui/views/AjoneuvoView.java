package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fi.kurvinen.korjaamo.domain.Ajoneuvo;
import fi.kurvinen.korjaamo.domain.Asiakas;
import fi.kurvinen.korjaamo.domain.Vakuutus;
import fi.kurvinen.korjaamo.service.AjoneuvoService;
import fi.kurvinen.korjaamo.service.AsiakasService;
import fi.kurvinen.korjaamo.ui.MainLayout;
import fi.kurvinen.korjaamo.ui.components.Ilmoitus;
import fi.kurvinen.korjaamo.ui.components.LomakeDialogi;
import fi.kurvinen.korjaamo.ui.components.Rekkari;
import jakarta.annotation.security.RolesAllowed;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Ajoneuvorekisteri – vaatimus 1 (ensimmäinen entiteetti), vain SUPER- ja USER-rooleille.
 * <p>
 * Gridissä näkyvät molemmat relaatiot:
 * <ul>
 *   <li>"Vakuutus (1:1)" – vaatimus 2</li>
 *   <li>"Omistaja" – N:1-puoli asiakkaan 1:N-relaatiosta (vaatimus 3)</li>
 * </ul>
 */
@Route(value = "ajoneuvot", layout = MainLayout.class)
@PageTitle("Ajoneuvot | Korjaamo")
@RolesAllowed({"SUPER", "USER"})
@AccessDeniedErrorRouter
public class AjoneuvoView extends VerticalLayout {

    private final AjoneuvoService service;
    private final Grid<Ajoneuvo> grid = new Grid<>(Ajoneuvo.class, false);
    private final LomakeDialogi<Ajoneuvo> dialogi = new LomakeDialogi<>(Ajoneuvo.class, "Uusi ajoneuvo", "Muokkaa ajoneuvoa");
    private final ComboBox<Asiakas> omistaja = new ComboBox<>("Omistaja");
    private AsiakasService asiakasService;

    public AjoneuvoView(AjoneuvoService service, AsiakasService asiakasService) {
        this.service = service;
        this.asiakasService = asiakasService;
        setSizeFull();

        Button uusi = new Button("Uusi ajoneuvo", VaadinIcon.PLUS.create(), e -> avaa(null));
        uusi.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout ylapalkki = new HorizontalLayout(new H2("Ajoneuvot"), uusi);
        ylapalkki.setWidthFull();
        ylapalkki.setAlignItems(FlexComponent.Alignment.BASELINE);
        ylapalkki.expand(ylapalkki.getComponentAt(0));

        NumberFormat km = NumberFormat.getIntegerInstance(Locale.of("fi", "FI"));
        grid.addComponentColumn(a -> new Rekkari(a.getRekisterinumero())).setHeader("Rekkari").setAutoWidth(true)
                .setFlexGrow(0).setComparator(Ajoneuvo::getRekisterinumero);
        grid.addColumn(a -> a.getMerkki() + " " + a.getMalli()).setHeader("Merkki ja malli").setSortable(true).setAutoWidth(true);
        grid.addColumn(Ajoneuvo::getVuosimalli).setHeader("Vuosi").setSortable(true).setAutoWidth(true);
        grid.addColumn(a -> km.format(a.getKilometrit()) + " km").setHeader("Mittarilukema")
                .setComparator(Ajoneuvo::getKilometrit).setAutoWidth(true);
        grid.addColumn(a -> a.getOmistaja().getKokoNimi()).setHeader("Omistaja").setSortable(true).setAutoWidth(true);
        grid.addComponentColumn(a -> vakuutusMerkki(a.getVakuutus())).setHeader("Vakuutus (1:1)").setAutoWidth(true);
        grid.addColumn(Ajoneuvo::getVin).setHeader("VIN").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();
        grid.addItemClickListener(e -> avaa(e.getItem()));

        luoLomake(asiakasService);
        add(ylapalkki, grid);
        paivita();
    }

    private Span vakuutusMerkki(Vakuutus v) {
        if (v == null) {
            Span s = new Span("Ei vakuutusta");
            s.getElement().getThemeList().add("badge error");
            return s;
        }
        Span s = new Span(v.getYhtio() + " · " + v.getTyyppi().getNimi());
        s.getElement().getThemeList().add(v.isVoimassa() ? "badge success" : "badge contrast");
        s.getElement().setAttribute("title", v.getVakuutusnumero() + (v.isVoimassa() ? " (voimassa)" : " (ei voimassa)"));
        return s;
    }

    private void luoLomake(AsiakasService asiakasService) {
        TextField rekisterinumero = new TextField("Rekisterinumero");
        rekisterinumero.setPlaceholder("ABC-123");
        rekisterinumero.setMaxLength(7);
        TextField merkki = new TextField("Merkki");
        TextField malli = new TextField("Malli");
        IntegerField vuosimalli = new IntegerField("Vuosimalli");
        vuosimalli.setStepButtonsVisible(true);
        IntegerField kilometrit = new IntegerField("Kilometrit");
        kilometrit.setSuffixComponent(new Span("km"));
        TextField vin = new TextField("VIN (valmistenumero)");
        vin.setMaxLength(17);
        omistaja.setItemLabelGenerator(a -> a.getKokoNimi() + " (" + a.getKaupunki() + ")");

        var b = dialogi.getBinder();
        // Rekisterinumero muutetaan isoiksi kirjaimiksi jo lomakkeella, jotta @Pattern hyväksyy "abc-123".
        b.forField(rekisterinumero).withNullRepresentation("").withConverter(s -> s == null ? null : s.trim().toUpperCase(Locale.ROOT), s -> s)
                .bind("rekisterinumero");
        b.forField(merkki).bind("merkki");
        b.forField(malli).bind("malli");
        b.forField(vuosimalli).bind("vuosimalli");
        b.forField(kilometrit).bind("kilometrit");
        b.forField(vin).withNullRepresentation("").withConverter(s -> s == null ? null : s.trim().toUpperCase(Locale.ROOT), s -> s).bind("vin");
        b.forField(omistaja).bind("omistaja");
        dialogi.lisaa(rekisterinumero, omistaja, merkki, malli, vuosimalli, kilometrit);
        dialogi.lisaaLeveana(vin);

        dialogi.kunTallennetaan(a -> {
            service.tallenna(a);
            Ilmoitus.onnistui("Ajoneuvo " + a.getRekisterinumero() + " tallennettu");
            paivita();
        });
        dialogi.kunPoistetaan(a -> {
            service.poista(a);
            Ilmoitus.onnistui("Ajoneuvo poistettu");
            paivita();
        });
    }

    /** Asiakaslista päivitetään ennen lomakkeen lukemista, jotta uudet asiakkaat ovat valittavissa. */
    private void avaa(Ajoneuvo a) {
        omistaja.setItems(asiakasService.kaikki());
        dialogi.avaa(a, Ajoneuvo::new);
    }

    private void paivita() {
        grid.setItems(service.kaikki());
    }
}
