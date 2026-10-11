package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fi.kurvinen.korjaamo.domain.Ajoneuvo;
import fi.kurvinen.korjaamo.domain.Vakuutus;
import fi.kurvinen.korjaamo.domain.VakuutusTyyppi;
import fi.kurvinen.korjaamo.service.AjoneuvoService;
import fi.kurvinen.korjaamo.service.VakuutusService;
import fi.kurvinen.korjaamo.ui.MainLayout;
import fi.kurvinen.korjaamo.ui.components.Ilmoitus;
import fi.kurvinen.korjaamo.ui.components.LomakeDialogi;
import fi.kurvinen.korjaamo.ui.components.Rekkari;
import jakarta.annotation.security.RolesAllowed;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Vakuutukset – vaatimus 2: 1:1-relaatio ajoneuvoon. Relaatio näkyy gridin sarakkeessa
 * "Ajoneuvo (1:1)". Lomakkeen ajoneuvolistassa näytetään vain ajoneuvot, joilla ei vielä ole
 * vakuutusta (sekä muokattavan vakuutuksen oma ajoneuvo), jolloin 1:1-sääntöä on vaikea rikkoa.
 */
@Route(value = "vakuutukset", layout = MainLayout.class)
@PageTitle("Vakuutukset | Korjaamo")
@RolesAllowed({"SUPER", "USER"})
@AccessDeniedErrorRouter
public class VakuutusView extends VerticalLayout {

    private static final DateTimeFormatter PVM = DateTimeFormatter.ofPattern("d.M.yyyy");

    private final VakuutusService service;
    private final AjoneuvoService ajoneuvoService;
    private final Grid<Vakuutus> grid = new Grid<>(Vakuutus.class, false);
    private final LomakeDialogi<Vakuutus> dialogi = new LomakeDialogi<>(Vakuutus.class, "Uusi vakuutus", "Muokkaa vakuutusta");
    private final ComboBox<Ajoneuvo> ajoneuvo = new ComboBox<>("Ajoneuvo");

    public VakuutusView(VakuutusService service, AjoneuvoService ajoneuvoService) {
        this.service = service;
        this.ajoneuvoService = ajoneuvoService;
        setSizeFull();

        Button uusi = new Button("Uusi vakuutus", VaadinIcon.PLUS.create(), e -> avaa(null));
        uusi.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout ylapalkki = new HorizontalLayout(new H2("Vakuutukset"), uusi);
        ylapalkki.setWidthFull();
        ylapalkki.setAlignItems(FlexComponent.Alignment.BASELINE);
        ylapalkki.expand(ylapalkki.getComponentAt(0));

        NumberFormat euro = NumberFormat.getCurrencyInstance(Locale.of("fi", "FI"));
        grid.addColumn(Vakuutus::getVakuutusnumero).setHeader("Vakuutusnumero").setAutoWidth(true).setSortable(true);
        grid.addColumn(Vakuutus::getYhtio).setHeader("Yhtiö").setAutoWidth(true).setSortable(true);
        grid.addColumn(v -> v.getTyyppi().getNimi()).setHeader("Tyyppi").setAutoWidth(true);
        grid.addComponentColumn(v -> {
            HorizontalLayout h = new HorizontalLayout(new Rekkari(v.getAjoneuvo().getRekisterinumero()),
                    new Span(v.getAjoneuvo().getMerkki() + " " + v.getAjoneuvo().getMalli()));
            h.setAlignItems(FlexComponent.Alignment.CENTER);
            h.setSpacing(false);
            return h;
        }).setHeader("Ajoneuvo (1:1)").setAutoWidth(true);
        grid.addColumn(v -> v.getAjoneuvo().getOmistaja().getKokoNimi()).setHeader("Omistaja").setAutoWidth(true);
        grid.addColumn(v -> PVM.format(v.getAlkaen()) + " – " + PVM.format(v.getPaattyy())).setHeader("Voimassa")
                .setComparator(Vakuutus::getPaattyy).setAutoWidth(true);
        grid.addComponentColumn(v -> {
            Span s = new Span(v.isVoimassa() ? "Voimassa" : (v.getPaattyy().isBefore(LocalDate.now()) ? "Päättynyt" : "Alkaa myöhemmin"));
            s.getElement().getThemeList().add(v.isVoimassa() ? "badge success" : "badge error");
            return s;
        }).setHeader("Tila").setAutoWidth(true);
        grid.addColumn(v -> euro.format(v.getOmavastuu())).setHeader("Omavastuu").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();
        grid.addItemClickListener(e -> avaa(e.getItem()));

        luoLomake();
        add(ylapalkki, grid);
        paivita();
    }

    private void avaa(Vakuutus v) {
        List<Ajoneuvo> vaihtoehdot = new ArrayList<>(ajoneuvoService.ilmanVakuutusta());
        if (v != null) {
            vaihtoehdot.add(0, v.getAjoneuvo());
        }
        ajoneuvo.setItems(vaihtoehdot);
        dialogi.avaa(v, () -> {
            Vakuutus uusi = new Vakuutus();
            uusi.setAlkaen(LocalDate.now());
            uusi.setPaattyy(LocalDate.now().plusYears(1));
            uusi.setTyyppi(VakuutusTyyppi.LIIKENNE);
            return uusi;
        });
    }

    private void luoLomake() {
        TextField yhtio = new TextField("Vakuutusyhtiö");
        TextField numero = new TextField("Vakuutusnumero");
        numero.setPlaceholder("LT12345678");
        Select<VakuutusTyyppi> tyyppi = new Select<>();
        tyyppi.setLabel("Tyyppi");
        tyyppi.setItems(VakuutusTyyppi.values());
        tyyppi.setItemLabelGenerator(VakuutusTyyppi::getNimi);
        DatePicker alkaen = new DatePicker("Alkaen");
        DatePicker paattyy = new DatePicker("Päättyy");
        BigDecimalField omavastuu = new BigDecimalField("Omavastuu (€)");
        ajoneuvo.setItemLabelGenerator(a -> a.getRekisterinumero() + " – " + a.getMerkki() + " " + a.getMalli());

        var b = dialogi.getBinder();
        b.forField(yhtio).bind("yhtio");
        b.forField(numero).withNullRepresentation("").withConverter(s -> s == null ? null : s.trim().toUpperCase(Locale.ROOT), s -> s).bind("vakuutusnumero");
        b.forField(tyyppi).bind("tyyppi");
        b.forField(alkaen).bind("alkaen");
        b.forField(paattyy).bind("paattyy");
        b.forField(omavastuu).bind("omavastuu");
        b.forField(ajoneuvo).bind("ajoneuvo");
        dialogi.lisaaLeveana(ajoneuvo);
        dialogi.lisaa(yhtio, numero, tyyppi, omavastuu, alkaen, paattyy);

        dialogi.kunTallennetaan(v -> {
            service.tallenna(v);
            Ilmoitus.onnistui("Vakuutus tallennettu");
            paivita();
        });
        dialogi.kunPoistetaan(v -> {
            service.poista(v);
            Ilmoitus.onnistui("Vakuutus poistettu");
            paivita();
        });
    }

    private void paivita() {
        grid.setItems(service.kaikki());
    }
}
