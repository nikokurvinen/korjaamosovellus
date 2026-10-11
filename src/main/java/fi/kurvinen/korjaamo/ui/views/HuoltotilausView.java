package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.Registration;
import fi.kurvinen.korjaamo.domain.Ajoneuvo;
import fi.kurvinen.korjaamo.domain.HuoltoTila;
import fi.kurvinen.korjaamo.domain.Huoltotilaus;
import fi.kurvinen.korjaamo.domain.Liite;
import fi.kurvinen.korjaamo.domain.Varaosa;
import fi.kurvinen.korjaamo.service.AjoneuvoService;
import fi.kurvinen.korjaamo.service.Broadcaster;
import fi.kurvinen.korjaamo.service.HistoriaService;
import fi.kurvinen.korjaamo.service.HuoltotilausService;
import fi.kurvinen.korjaamo.service.LiiketoimintaVirhe;
import fi.kurvinen.korjaamo.service.TiedostoService;
import fi.kurvinen.korjaamo.service.VaraosaService;
import fi.kurvinen.korjaamo.ui.MainLayout;
import fi.kurvinen.korjaamo.ui.components.HistoriaLista;
import fi.kurvinen.korjaamo.ui.components.Ilmoitus;
import fi.kurvinen.korjaamo.ui.components.PoistoVahvistus;
import fi.kurvinen.korjaamo.ui.components.Rekkari;
import fi.kurvinen.korjaamo.ui.components.TilaBadge;
import jakarta.annotation.security.PermitAll;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Huoltotilausten hallinta – kaikille kirjautuneille.
 * <p>
 * Layout-tyyppi (vaatimus 12c): <b>master–detail</b>. {@link SplitLayout} jakaa näkymän listaan
 * (vasen) ja valitun tilauksen tietoihin (oikea). Oikea puoli on {@link TabSheet}: lomake,
 * liitetiedostot (vaatimus 28) ja tilauksen oma muutoshistoria (vaatimus 32).
 * <p>
 * CRUD: listaus (Grid), uusi tilaus ja muokkaus samalla lomakkeella, poisto vahvistuksella.
 * M:N-relaatio (vaatimus 4) näkyy gridin sarakkeessa "Varaosat" ja lomakkeen MultiSelectComboBoxissa.
 */
@Route(value = "huollot/:id?", layout = MainLayout.class)
@PageTitle("Huoltotilaukset | Korjaamo")
@PermitAll
public class HuoltotilausView extends SplitLayout implements BeforeEnterObserver {

    private static final DateTimeFormatter PVM = DateTimeFormatter.ofPattern("d.M.yyyy");
    private static final NumberFormat EURO = NumberFormat.getCurrencyInstance(Locale.of("fi", "FI"));

    private final HuoltotilausService service;
    private final TiedostoService tiedostoService;
    private final HistoriaService historiaService;
    private final Broadcaster broadcaster;

    private final Grid<Huoltotilaus> grid = new Grid<>(Huoltotilaus.class, false);
    private final TextField suodatin = new TextField();
    private List<Huoltotilaus> kaikki = List.of();

    // Lomake
    private final BeanValidationBinder<Huoltotilaus> binder = new BeanValidationBinder<>(Huoltotilaus.class);
    private final TextField otsikko = new TextField("Otsikko");
    private final ComboBox<Ajoneuvo> ajoneuvo = new ComboBox<>("Ajoneuvo");
    private final Select<HuoltoTila> tila = new Select<>();
    private final DatePicker vastaanotettu = new DatePicker("Vastaanotettu");
    private final DatePicker valmistuu = new DatePicker("Valmistuu (arvio)");
    private final BigDecimalField tyotunnit = new BigDecimalField("Työtunnit (h)");
    private final BigDecimalField hinta = new BigDecimalField("Hinta (€)");
    private final MultiSelectComboBox<Varaosa> varaosat = new MultiSelectComboBox<>("Käytetyt varaosat");
    private final TextArea kuvaus = new TextArea("Työn kuvaus");
    private final Span lomakeVirhe = new Span();
    private final Span auditointi = new Span();
    private final Button poista = new Button("Poista", VaadinIcon.TRASH.create());

    private final H3 detailOtsikko = new H3();
    private final TabSheet valilehdet = new TabSheet();
    private final VerticalLayout liitteet = new VerticalLayout();
    private final HistoriaLista historia = new HistoriaLista();
    private final VerticalLayout detail = new VerticalLayout();
    private final Div tyhjaDetail = new Div();

    private Huoltotilaus valittu;
    /** Push-päivityksen uudelleenvalinta ei saa ladata lomaketta uudelleen kesken käyttäjän muokkausten. */
    private boolean ohitaValinta;
    private Registration rekisterointi;

    public HuoltotilausView(HuoltotilausService service, AjoneuvoService ajoneuvoService, VaraosaService varaosaService,
                            TiedostoService tiedostoService, HistoriaService historiaService, Broadcaster broadcaster) {
        this.service = service;
        this.tiedostoService = tiedostoService;
        this.historiaService = historiaService;
        this.broadcaster = broadcaster;
        setSizeFull();
        setSplitterPosition(60);

        ajoneuvo.setItems(ajoneuvoService.kaikki());
        ajoneuvo.setItemLabelGenerator(a -> a.getRekisterinumero() + " – " + a.getMerkki() + " " + a.getMalli());
        varaosat.setItems(varaosaService.kaikki());
        varaosat.setItemLabelGenerator(v -> v.getNimi() + " (" + v.getTuotenumero() + ")");

        addToPrimary(luoLista());
        addToSecondary(luoDetail());
        paivitaLista();
        naytaTilaus(null);
    }

    // ---------------------------------------------------------------- Master: lista

    private VerticalLayout luoLista() {
        suodatin.setPlaceholder("Suodata rekisterinumerolla tai otsikolla…");
        suodatin.setPrefixComponent(VaadinIcon.FILTER.create());
        suodatin.setClearButtonVisible(true);
        suodatin.setValueChangeMode(ValueChangeMode.LAZY);
        suodatin.addValueChangeListener(e -> suodata());
        suodatin.setWidthFull();

        Button uusi = new Button("Uusi tilaus", VaadinIcon.PLUS.create(), e -> {
            grid.asSingleSelect().clear();
            naytaTilaus(uusiTilaus());
        });
        uusi.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        grid.addComponentColumn(h -> new Rekkari(h.getAjoneuvo().getRekisterinumero()))
                .setHeader("Rekkari").setAutoWidth(true).setFlexGrow(0)
                .setComparator(h -> h.getAjoneuvo().getRekisterinumero());
        grid.addColumn(Huoltotilaus::getOtsikko).setHeader("Otsikko").setWidth("190px").setFlexGrow(2).setSortable(true)
                .setTooltipGenerator(Huoltotilaus::getOtsikko);
        grid.addComponentColumn(h -> new TilaBadge(h.getTila())).setHeader("Tila").setAutoWidth(true)
                .setComparator(Huoltotilaus::getTila);
        grid.addColumn(h -> h.getAjoneuvo().getOmistaja().getKokoNimi()).setHeader("Asiakas").setAutoWidth(true).setSortable(true);
        // M:N-relaatio: määrä näkyvissä, osien nimet tooltipissä.
        grid.addColumn(h -> h.getVaraosat().size() + " kpl").setHeader("Varaosat").setAutoWidth(true)
                .setTooltipGenerator(h -> h.getVaraosat().isEmpty() ? "Ei varaosia"
                        : h.getVaraosat().stream().map(Varaosa::getNimi).sorted().collect(Collectors.joining("\n")));
        grid.addColumn(h -> PVM.format(h.getVastaanotettu())).setHeader("Vastaanotettu").setAutoWidth(true)
                .setComparator(Huoltotilaus::getVastaanotettu);
        grid.addColumn(h -> EURO.format(h.getHinta())).setHeader("Hinta").setAutoWidth(true)
                .setComparator(Huoltotilaus::getHinta);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setSizeFull();
        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null && !ohitaValinta) {
                service.haeKokonaan(e.getValue().getId()).ifPresent(this::naytaTilaus);
                getUI().ifPresent(ui -> ui.getPage().getHistory().replaceState(null, "huollot/" + e.getValue().getId()));
            }
        });

        HorizontalLayout tyokalut = new HorizontalLayout(suodatin, uusi);
        tyokalut.setWidthFull();
        tyokalut.expand(suodatin);
        VerticalLayout lista = new VerticalLayout(tyokalut, grid);
        lista.setSizeFull();
        return lista;
    }

    private void paivitaLista() {
        kaikki = service.kaikki();
        suodata();
    }

    private void suodata() {
        String s = suodatin.getValue().trim().toLowerCase(Locale.ROOT);
        grid.setItems(s.isEmpty() ? kaikki : kaikki.stream()
                .filter(h -> h.getOtsikko().toLowerCase(Locale.ROOT).contains(s)
                        || h.getAjoneuvo().getRekisterinumero().toLowerCase(Locale.ROOT).contains(s))
                .toList());
    }

    // ---------------------------------------------------------------- Detail: lomake ja välilehdet

    private VerticalLayout luoDetail() {
        tila.setLabel("Tila");
        tila.setItems(HuoltoTila.values());
        tila.setItemLabelGenerator(HuoltoTila::getNimi);
        ajoneuvo.setRequiredIndicatorVisible(true);
        kuvaus.setMaxLength(2000);
        kuvaus.setValueChangeMode(ValueChangeMode.EAGER);
        kuvaus.addValueChangeListener(e -> e.getSource().setHelperText(e.getValue().length() + " / 2000"));
        kuvaus.setMinHeight("120px");
        tyotunnit.setSuffixComponent(new Span("h"));
        hinta.setSuffixComponent(new Span("€"));
        valmistuu.setClearButtonVisible(true);

        // bind(String) käyttää entiteetin Bean Validation -annotaatioita (BeanValidationBinder).
        binder.forField(otsikko).bind("otsikko");
        binder.forField(ajoneuvo).bind("ajoneuvo");
        binder.forField(tila).bind("tila");
        binder.forField(vastaanotettu).bind("vastaanotettu");
        binder.forField(valmistuu).bind("valmistuu");
        binder.forField(tyotunnit).bind("tyotunnit");
        binder.forField(hinta).bind("hinta");
        binder.forField(varaosat).bind("varaosat");
        binder.forField(kuvaus).withNullRepresentation("").bind("kuvaus");

        FormLayout lomake = new FormLayout(otsikko, ajoneuvo, tila, vastaanotettu, valmistuu, tyotunnit, hinta, varaosat, kuvaus);
        lomake.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("420px", 2));
        lomake.setColspan(otsikko, 2);
        lomake.setColspan(varaosat, 2);
        lomake.setColspan(kuvaus, 2);

        lomakeVirhe.getStyle().set("color", "var(--lumo-error-text-color)");
        auditointi.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("color", "var(--lumo-secondary-text-color)");

        Button tallenna = new Button("Tallenna", VaadinIcon.CHECK.create(), e -> tallenna());
        tallenna.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button peruuta = new Button("Peruuta", e -> {
            grid.asSingleSelect().clear();
            naytaTilaus(null);
        });
        poista.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        poista.addClickListener(e -> PoistoVahvistus.kysy("tilaus " + valittu.getOtsikko(), () -> {
            service.poista(valittu);
            Ilmoitus.onnistui("Tilaus poistettu");
            naytaTilaus(null);
            paivitaLista();
        }));
        HorizontalLayout napit = new HorizontalLayout(poista, peruuta, tallenna);
        napit.setWidthFull();
        napit.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        poista.getStyle().set("margin-inline-end", "auto");

        VerticalLayout tiedot = new VerticalLayout(lomake, lomakeVirhe, napit, auditointi);
        tiedot.setPadding(false);

        valilehdet.add("Tiedot", tiedot);
        valilehdet.add("Liitteet", liitteet);
        valilehdet.add("Historia", historia);
        valilehdet.setWidthFull();
        valilehdet.addSelectedChangeListener(e -> {
            if (e.getSelectedTab().getLabel().equals("Historia") && valittu != null && !valittu.isNew()) {
                historia.nayta(historiaService.entiteetinHistoria(Huoltotilaus.class, valittu.getId()), false);
            }
        });

        detailOtsikko.getStyle().set("margin", "0");
        detail.add(detailOtsikko, valilehdet);
        detail.setPadding(true);

        tyhjaDetail.add(VaadinIcon.WRENCH.create(), new Span(" Valitse tilaus listalta tai luo uusi."));
        tyhjaDetail.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin", "auto");
        VerticalLayout kaare = new VerticalLayout(detail, tyhjaDetail);
        kaare.setSizeFull();
        kaare.setPadding(false);
        kaare.getStyle().set("overflow", "auto").set("background", "var(--lumo-base-color)");
        return kaare;
    }

    private Huoltotilaus uusiTilaus() {
        Huoltotilaus h = new Huoltotilaus();
        h.setVastaanotettu(LocalDate.now());
        h.setTila(HuoltoTila.JONOSSA);
        return h;
    }

    private void naytaTilaus(Huoltotilaus h) {
        valittu = h;
        detail.setVisible(h != null);
        tyhjaDetail.setVisible(h == null);
        lomakeVirhe.setText("");
        if (h == null) {
            return;
        }
        binder.readBean(h);
        detailOtsikko.setText(h.isNew() ? "Uusi huoltotilaus" : "Tilaus #" + h.getId() + " – " + h.getAjoneuvo().getRekisterinumero());
        poista.setVisible(!h.isNew());
        valilehdet.getTabAt(1).setEnabled(!h.isNew());
        valilehdet.getTabAt(2).setEnabled(!h.isNew());
        valilehdet.setSelectedIndex(0);
        auditointi.setText(h.isNew() ? "" : "Luonut " + h.getLuonut() + " · viimeksi muokannut " + h.getMuokannut()
                + " (" + DateTimeFormatter.ofPattern("d.M.yyyy HH:mm").format(h.getMuokattu()) + ")");
        if (!h.isNew()) {
            paivitaLiitteet();
        }
    }

    private void tallenna() {
        try {
            binder.writeBean(valittu);
            Huoltotilaus tallennettu = service.tallenna(valittu);
            Ilmoitus.onnistui("Tilaus tallennettu");
            paivitaLista();
            service.haeKokonaan(tallennettu.getId()).ifPresent(this::naytaTilaus);
            grid.select(kaikki.stream().filter(x -> x.getId().equals(tallennettu.getId())).findFirst().orElse(null));
        } catch (ValidationException e) {
            lomakeVirhe.setText(e.getBeanValidationErrors().stream().map(ValidationResult::getErrorMessage)
                    .collect(Collectors.joining(" ")));
        } catch (LiiketoimintaVirhe e) {
            Ilmoitus.virhe(e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            Ilmoitus.virhe("Toinen käyttäjä muokkasi tilausta samaan aikaan. Tiedot ladattiin uudelleen.");
            service.haeKokonaan(valittu.getId()).ifPresent(this::naytaTilaus);
        }
    }

    // ---------------------------------------------------------------- Liitteet (vaatimus 28)

    private void paivitaLiitteet() {
        liitteet.removeAll();
        Long tilausId = valittu.getId();
        UI ui = UI.getCurrent();

        /*
         * UploadHandler.inMemory: tiedosto luetaan muistiin (max 10 Mt) ja callback ajetaan
         * upload-pyynnön säikeessä, jossa Spring Securityn konteksti on voimassa – auditointi
         * saa siis oikean käyttäjän. Käyttöliittymä päivitetään ui.access()-kutsulla.
         */
        Upload upload = new Upload(UploadHandler.inMemory((meta, data) -> {
            try {
                tiedostoService.tallenna(tilausId, meta.fileName(), meta.contentType(), data);
                ui.access(() -> {
                    Ilmoitus.onnistui("Liite " + meta.fileName() + " tallennettu");
                    paivitaLiitteet();
                });
            } catch (LiiketoimintaVirhe e) {
                ui.access(() -> Ilmoitus.virhe(e.getMessage()));
            }
        }));
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/webp", "application/pdf", "text/plain",
                ".png", ".jpg", ".jpeg", ".webp", ".pdf", ".txt");
        upload.setMaxFileSize((int) Liite.MAX_KOKO);
        upload.setMaxFiles(5);
        upload.setDropLabel(new Span("Pudota kuitti, kuva tai PDF tähän (max 10 Mt)"));
        upload.setUploadButton(new Button("Lisää liitteitä", VaadinIcon.PAPERCLIP.create()));
        upload.addFileRejectedListener(e -> Ilmoitus.virhe(e.getErrorMessage()));
        upload.setWidthFull();

        Grid<Liite> liiteGrid = new Grid<>(Liite.class, false);
        liiteGrid.addColumn(new ComponentRenderer<>(l -> {
            Anchor a = new Anchor(DownloadHandler.fromInputStream(ev -> {
                try {
                    return new DownloadResponse(tiedostoService.avaa(l), l.getTiedostonimi(), l.getSisaltotyyppi(), l.getKoko());
                } catch (IOException ex) {
                    return DownloadResponse.error(404, "Tiedosto puuttuu levyltä");
                }
            }), l.getTiedostonimi());
            a.setDownload(true);
            return a;
        })).setHeader("Tiedosto").setFlexGrow(2);
        liiteGrid.addColumn(l -> l.getKoko() < 1024 ? l.getKoko() + " t" : (l.getKoko() / 1024) + " kt").setHeader("Koko").setAutoWidth(true);
        liiteGrid.addColumn(l -> l.getLisannyt() + ", " + DateTimeFormatter.ofPattern("d.M.yyyy HH:mm").format(l.getLisatty()))
                .setHeader("Lisätty").setAutoWidth(true);
        liiteGrid.addComponentColumn(l -> {
            Button p = new Button(VaadinIcon.TRASH.create(), e -> PoistoVahvistus.kysy("liite " + l.getTiedostonimi(), () -> {
                tiedostoService.poista(l);
                paivitaLiitteet();
            }));
            p.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
            p.setAriaLabel("Poista liite");
            return p;
        }).setAutoWidth(true).setFlexGrow(0);
        List<Liite> lista = tiedostoService.liitteet(valittu);
        liiteGrid.setItems(lista);
        liiteGrid.setAllRowsVisible(true);
        liiteGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        liitteet.add(upload, lista.isEmpty() ? new Span("Ei liitteitä.") : liiteGrid);
        liitteet.setPadding(false);
    }

    // ---------------------------------------------------------------- Reititys ja Push

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getRouteParameters().getLong("id")
                .flatMap(service::haeKokonaan)
                .ifPresent(h -> {
                    naytaTilaus(h);
                    kaikki.stream().filter(x -> x.getId().equals(h.getId())).findFirst().ifPresent(grid::select);
                });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        rekisterointi = broadcaster.rekisteroi(t -> {
            if ("Huoltotilaus".equals(t.kohde())) {
                ui.access(() -> {
                    Huoltotilaus valinta = grid.asSingleSelect().getValue();
                    paivitaLista();
                    if (valinta != null) {
                        ohitaValinta = true;
                        kaikki.stream().filter(x -> x.getId().equals(valinta.getId())).findFirst().ifPresent(grid::select);
                        ohitaValinta = false;
                    }
                });
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (rekisterointi != null) rekisterointi.remove();
    }
}
