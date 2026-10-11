package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import fi.kurvinen.korjaamo.domain.Varaosa;
import fi.kurvinen.korjaamo.service.CsvService;
import fi.kurvinen.korjaamo.service.LiiketoimintaVirhe;
import fi.kurvinen.korjaamo.service.VaraosaService;
import fi.kurvinen.korjaamo.ui.MainLayout;
import fi.kurvinen.korjaamo.ui.components.Ilmoitus;
import fi.kurvinen.korjaamo.ui.components.LomakeDialogi;
import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayInputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Varaosaluettelo – vaatimus 4 (M:N huoltotilauksiin) ja vaatimus 29 (CSV-tuonti ja -vienti).
 * Vain SUPER- ja USER-rooleille.
 */
@Route(value = "varaosat", layout = MainLayout.class)
@PageTitle("Varaosat | Korjaamo")
@RolesAllowed({"SUPER", "USER"})
@AccessDeniedErrorRouter
public class VaraosaView extends VerticalLayout {

    private final VaraosaService service;
    private final CsvService csvService;
    private final Grid<Varaosa> grid = new Grid<>(Varaosa.class, false);
    private final LomakeDialogi<Varaosa> dialogi = new LomakeDialogi<>(Varaosa.class, "Uusi varaosa", "Muokkaa varaosaa");

    public VaraosaView(VaraosaService service, CsvService csvService) {
        this.service = service;
        this.csvService = csvService;
        setSizeFull();

        Button uusi = new Button("Uusi varaosa", VaadinIcon.PLUS.create(), e -> dialogi.avaa(null, Varaosa::new));
        uusi.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // CSV-vienti: DownloadHandler muodostaa tiedoston vasta, kun käyttäjä klikkaa linkkiä,
        // joten ladattu tiedosto on aina ajan tasalla.
        Anchor vie = new Anchor(DownloadHandler.fromInputStream(ev -> {
            byte[] csv = csvService.vieVaraosat();
            return new DownloadResponse(new ByteArrayInputStream(csv), "varaosat-" + LocalDate.now() + ".csv",
                    "text/csv;charset=UTF-8", csv.length);
        }), "");
        vie.setDownload(true);
        Button vieNappi = new Button("Vie CSV", VaadinIcon.DOWNLOAD.create());
        vie.add(vieNappi);

        Button tuo = new Button("Tuo CSV", VaadinIcon.UPLOAD.create(), e -> avaaTuonti());

        HorizontalLayout ylapalkki = new HorizontalLayout(new H2("Varaosat"), vie, tuo, uusi);
        ylapalkki.setWidthFull();
        ylapalkki.setAlignItems(FlexComponent.Alignment.BASELINE);
        ylapalkki.expand(ylapalkki.getComponentAt(0));
        ylapalkki.getStyle().set("flex-wrap", "wrap");

        NumberFormat euro = NumberFormat.getCurrencyInstance(Locale.of("fi", "FI"));
        grid.addColumn(Varaosa::getTuotenumero).setHeader("Tuotenumero").setSortable(true).setAutoWidth(true);
        grid.addColumn(Varaosa::getNimi).setHeader("Nimi").setSortable(true).setFlexGrow(2);
        grid.addColumn(Varaosa::getValmistaja).setHeader("Valmistaja").setSortable(true).setAutoWidth(true);
        grid.addColumn(v -> euro.format(v.getHinta())).setHeader("Hinta").setComparator(Varaosa::getHinta).setAutoWidth(true);
        grid.addComponentColumn(v -> {
            Span s = new Span(v.getVarastosaldo() + " kpl");
            s.getElement().getThemeList().add(v.getVarastosaldo() < 5 ? "badge error" : "badge success");
            return s;
        }).setHeader("Varastossa").setComparator(Varaosa::getVarastosaldo).setAutoWidth(true);
        grid.addColumn(v -> v.getMuokannut()).setHeader("Muokannut").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();
        grid.addItemClickListener(e -> dialogi.avaa(e.getItem(), Varaosa::new));

        luoLomake();
        add(ylapalkki, grid);
        paivita();
    }

    private void luoLomake() {
        TextField tuotenumero = new TextField("Tuotenumero");
        tuotenumero.setPlaceholder("JAR-1042");
        TextField nimi = new TextField("Nimi");
        TextField valmistaja = new TextField("Valmistaja");
        BigDecimalField hinta = new BigDecimalField("Hinta (€)");
        IntegerField saldo = new IntegerField("Varastosaldo");
        saldo.setStepButtonsVisible(true);
        var b = dialogi.getBinder();
        b.forField(tuotenumero).withNullRepresentation("").withConverter(s -> s == null ? null : s.trim().toUpperCase(Locale.ROOT), s -> s).bind("tuotenumero");
        b.forField(nimi).bind("nimi");
        b.forField(valmistaja).bind("valmistaja");
        b.forField(hinta).bind("hinta");
        b.forField(saldo).bind("varastosaldo");
        dialogi.lisaa(tuotenumero, valmistaja);
        dialogi.lisaaLeveana(nimi);
        dialogi.lisaa(hinta, saldo);
        dialogi.kunTallennetaan(v -> {
            service.tallenna(v);
            Ilmoitus.onnistui("Varaosa tallennettu");
            paivita();
        });
        dialogi.kunPoistetaan(v -> {
            service.poista(v);
            Ilmoitus.onnistui("Varaosa poistettu");
            paivita();
        });
    }

    private void avaaTuonti() {
        Dialog d = new Dialog();
        d.setHeaderTitle("Tuo varaosat CSV-tiedostosta");
        d.setWidth("min(640px, 95vw)");
        VerticalLayout tulos = new VerticalLayout();
        tulos.setPadding(false);
        UI ui = UI.getCurrent();

        Upload upload = new Upload(UploadHandler.inMemory((meta, data) -> {
            try {
                CsvService.TuontiTulos t = csvService.tuoVaraosat(data);
                ui.access(() -> naytaTulos(tulos, t));
            } catch (LiiketoimintaVirhe e) {
                ui.access(() -> Ilmoitus.virhe(e.getMessage()));
            }
        }));
        upload.setAcceptedFileTypes(".csv", "text/csv");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(2 * 1024 * 1024);
        upload.setDropLabel(new Span("Pudota CSV-tiedosto tähän"));
        upload.setUploadButton(new Button("Valitse CSV-tiedosto", VaadinIcon.FILE_TABLE.create()));

        d.add(new Paragraph("Sarakkeet (puolipiste-eroteltuna, ensimmäinen rivi otsikot): "
                + "tuotenumero;nimi;valmistaja;hinta;varastosaldo. Olemassa olevat tuotenumerot päivitetään, "
                + "uudet lisätään. Virheelliset rivit ohitetaan ja raportoidaan."), upload, tulos);
        d.getFooter().add(new Button("Sulje", e -> d.close()));
        d.open();
    }

    private void naytaTulos(VerticalLayout alue, CsvService.TuontiTulos t) {
        alue.removeAll();
        Span yhteenveto = new Span(t.lisatty() + " lisätty, " + t.paivitetty() + " päivitetty, " + t.virheet().size() + " virheellistä riviä");
        yhteenveto.getElement().getThemeList().add(t.virheet().isEmpty() ? "badge success" : "badge error");
        alue.add(yhteenveto);
        if (!t.virheet().isEmpty()) {
            UnorderedList lista = new UnorderedList();
            t.virheet().forEach(v -> lista.add(new ListItem(v)));
            lista.getStyle().set("color", "var(--lumo-error-text-color)").set("font-size", "var(--lumo-font-size-s)");
            alue.add(lista);
        }
        paivita();
    }

    private void paivita() {
        grid.setItems(service.kaikki());
    }
}
