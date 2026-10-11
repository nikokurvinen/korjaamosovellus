package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import fi.kurvinen.korjaamo.domain.HuoltoTila;
import fi.kurvinen.korjaamo.domain.Huoltotilaus;
import fi.kurvinen.korjaamo.domain.Varaosa;
import fi.kurvinen.korjaamo.repository.HuoltoHakuehdot;
import fi.kurvinen.korjaamo.service.HuoltotilausService;
import fi.kurvinen.korjaamo.service.VaraosaService;
import fi.kurvinen.korjaamo.ui.MainLayout;
import fi.kurvinen.korjaamo.ui.components.Rekkari;
import fi.kurvinen.korjaamo.ui.components.TilaBadge;
import jakarta.annotation.security.PermitAll;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Edistynyt hakunäkymä (Suodattaminen, kohdat 1–5). Lomakkeen kentät kootaan
 * {@link HuoltoHakuehdot}-recordiksi, josta repository muodostaa dynaamisen CriteriaQueryn.
 * <p>
 * Tulosgrid on <b>lazy</b>: Grid pyytää vain näkyvät rivit (offset/limit), ja rivimäärä haetaan
 * erillisellä count-kyselyllä. Näin haku toimii myös tuhansilla tilauksilla.
 * <p>
 * Lomakkeen alla näytetään, millainen ehto kyselyyn muodostui – tämä havainnollistaa, että
 * predikaatteja lisätään vain täytetyille kentille.
 */
@Route(value = "haku", layout = MainLayout.class)
@PageTitle("Huoltohaku | Korjaamo")
@PermitAll
public class HakuView extends VerticalLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter PVM = DateTimeFormatter.ofPattern("d.M.yyyy");
    private static final NumberFormat EURO = NumberFormat.getCurrencyInstance(Locale.of("fi", "FI"));

    private final HuoltotilausService service;

    private final TextField rekisterinumero = new TextField("Rekisterinumero");
    private final TextField otsikko = new TextField("Otsikko sisältää");
    private final BigDecimalField minHinta = new BigDecimalField("Hinta vähintään (€)");
    private final DatePicker alkaen = new DatePicker("Vastaanotettu alkaen");
    private final DatePicker asti = new DatePicker("Vastaanotettu asti");
    private final ComboBox<Varaosa> varaosa = new ComboBox<>("Käytetty varaosa");
    private final TextField sukunimi = new TextField("Asiakkaan sukunimi");
    private final TextField hakusana = new TextField("Hakusana (otsikko TAI kuvaus)");
    private final Select<HuoltoTila> tila = new Select<>();

    private final Grid<Huoltotilaus> grid = new Grid<>(Huoltotilaus.class, false);
    private final Span tulosMaara = new Span();
    private final Pre ehtoKuvaus = new Pre();
    private HuoltoHakuehdot ehdot = HuoltoHakuehdot.tyhja();

    public HakuView(HuoltotilausService service, VaraosaService varaosaService) {
        this.service = service;
        setSizeFull();

        varaosa.setItems(varaosaService.kaikki());
        varaosa.setItemLabelGenerator(v -> v.getNimi() + " (" + v.getTuotenumero() + ")");
        varaosa.setClearButtonVisible(true);
        tila.setLabel("Tila (AND hakusanan kanssa)");
        tila.setItems(HuoltoTila.values());
        tila.setItemLabelGenerator(t -> t == null ? "Kaikki tilat" : t.getNimi());
        tila.setEmptySelectionAllowed(true);
        tila.setEmptySelectionCaption("Kaikki tilat");
        for (var f : List.of(rekisterinumero, otsikko, sukunimi, hakusana)) {
            f.setClearButtonVisible(true);
        }
        rekisterinumero.setPlaceholder("esim. ABC");
        hakusana.setPlaceholder("esim. jarru");
        hakusana.setPrefixComponent(VaadinIcon.SEARCH.create());
        alkaen.setClearButtonVisible(true);
        asti.setClearButtonVisible(true);

        FormLayout lomake = new FormLayout();
        lomake.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3));
        lomake.add(osio("1. Perustiedot"), rekisterinumero, otsikko, minHinta,
                osio("2. Päivämääräväli"), alkaen, asti, new Div(),
                osio("3–4. Relaatiot (JOIN)"), varaosa, sukunimi, new Div(),
                osio("5. (otsikko TAI kuvaus) JA tila"), hakusana, tila);
        lomake.getChildren().filter(c -> c instanceof H4).forEach(c -> lomake.setColspan(c, 3));

        Button hae = new Button("Hae", VaadinIcon.SEARCH.create(), e -> hae());
        hae.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        hae.addClickShortcut(Key.ENTER);
        Button tyhjenna = new Button("Tyhjennä", VaadinIcon.ERASER.create(), e -> {
            for (var f : List.of(rekisterinumero, otsikko, sukunimi, hakusana)) f.clear();
            minHinta.clear();
            alkaen.clear();
            asti.clear();
            varaosa.clear();
            tila.clear();
            hae();
        });
        tyhjenna.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        HorizontalLayout napit = new HorizontalLayout(hae, tyhjenna, tulosMaara);
        napit.setAlignItems(FlexComponent.Alignment.CENTER);

        ehtoKuvaus.getStyle().set("margin", "0").set("font-size", "var(--lumo-font-size-s)").set("white-space", "pre-wrap");
        Details ehtoDetails = new Details("Muodostettu hakuehto (Criteria API)", ehtoKuvaus);
        ehtoDetails.setOpened(true);

        grid.addComponentColumn(h -> new Rekkari(h.getAjoneuvo().getRekisterinumero())).setHeader("Rekkari").setAutoWidth(true).setFlexGrow(0);
        grid.addColumn(Huoltotilaus::getOtsikko).setHeader("Otsikko").setFlexGrow(2);
        grid.addComponentColumn(h -> new TilaBadge(h.getTila())).setHeader("Tila").setAutoWidth(true);
        grid.addColumn(h -> h.getAjoneuvo().getOmistaja().getKokoNimi()).setHeader("Asiakas").setAutoWidth(true);
        grid.addColumn(h -> h.getVaraosat().stream().map(Varaosa::getNimi).sorted().collect(Collectors.joining(", ")))
                .setHeader("Varaosat").setFlexGrow(2);
        grid.addColumn(h -> PVM.format(h.getVastaanotettu())).setHeader("Vastaanotettu").setAutoWidth(true);
        grid.addColumn(h -> EURO.format(h.getHinta())).setHeader("Hinta").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_WRAP_CELL_CONTENT);
        grid.setMinHeight("320px");
        grid.addItemClickListener(e -> getUI().ifPresent(ui ->
                ui.navigate(HuoltotilausView.class, new RouteParameters("id", String.valueOf(e.getItem().getId())))));

        add(new H2("Huoltotilausten haku"), lomake, napit, ehtoDetails, grid);
        setFlexGrow(1, grid);
        hae();
    }

    private H4 osio(String teksti) {
        H4 h = new H4(teksti);
        h.getStyle().set("margin", "var(--lumo-space-s) 0 0").set("color", "var(--lumo-primary-text-color)");
        return h;
    }

    private void hae() {
        ehdot = new HuoltoHakuehdot(rekisterinumero.getValue(), otsikko.getValue(), minHinta.getValue(),
                alkaen.getValue(), asti.getValue(), varaosa.getValue(), sukunimi.getValue(), hakusana.getValue(),
                tila.getValue());
        // Lazy-dataprovider: Grid kutsuu ensimmäistä callbackia sivu kerrallaan, toista rivimäärän laskemiseen.
        grid.setItems(
                q -> service.hae(ehdot, q.getOffset(), q.getLimit()).stream(),
                q -> (int) service.laske(ehdot));
        long maara = service.laske(ehdot);
        tulosMaara.setText(maara + " tilausta");
        ehtoKuvaus.setText(kuvaaEhto());
    }

    /** Ihmisluettava kuvaus muodostetusta WHERE-ehdosta (vastaa repositoryn predikaattilogiikkaa). */
    private String kuvaaEhto() {
        List<String> osat = new ArrayList<>();
        if (!rekisterinumero.isEmpty()) osat.add("upper(ajoneuvo.rekisterinumero) LIKE '%" + rekisterinumero.getValue().trim().toUpperCase() + "%'");
        if (!otsikko.isEmpty()) osat.add("lower(otsikko) LIKE '%" + otsikko.getValue().trim().toLowerCase() + "%'");
        if (minHinta.getValue() != null) osat.add("hinta >= " + minHinta.getValue());
        if (alkaen.getValue() != null && asti.getValue() != null) osat.add("vastaanotettu BETWEEN " + alkaen.getValue() + " AND " + asti.getValue());
        else if (alkaen.getValue() != null) osat.add("vastaanotettu >= " + alkaen.getValue());
        else if (asti.getValue() != null) osat.add("vastaanotettu <= " + asti.getValue());
        if (varaosa.getValue() != null) osat.add("JOIN varaosat v: v.id = " + varaosa.getValue().getId() + " (" + varaosa.getValue().getNimi() + ")");
        if (!sukunimi.isEmpty()) osat.add("JOIN ajoneuvo.omistaja o: lower(o.sukunimi) LIKE '%" + sukunimi.getValue().trim().toLowerCase() + "%'");
        if (!hakusana.isEmpty()) {
            String s = hakusana.getValue().trim().toLowerCase();
            osat.add("(lower(otsikko) LIKE '%" + s + "%' OR lower(kuvaus) LIKE '%" + s + "%')");
        }
        if (tila.getValue() != null) osat.add("tila = " + tila.getValue().name());
        return osat.isEmpty() ? "Ei ehtoja – WHERE-lausetta ei muodosteta, joten kaikki tilaukset palautetaan."
                : "WHERE " + String.join("\n  AND ", osat);
    }

    /** Dashboardin tilastokortti tai kaavio voi avata haun valmiiksi suodatettuna: /haku?tila=VALMIS */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getLocation().getQueryParameters().getSingleParameter("tila").ifPresent(t -> {
            try {
                tila.setValue(HuoltoTila.valueOf(t));
                hae();
            } catch (IllegalArgumentException ignored) {
                // Tuntematon tila osoiterivillä – näytetään suodattamaton haku.
            }
        });
    }
}
