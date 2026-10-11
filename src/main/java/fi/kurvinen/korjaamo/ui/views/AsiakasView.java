package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fi.kurvinen.korjaamo.domain.Asiakas;
import fi.kurvinen.korjaamo.service.AsiakasService;
import fi.kurvinen.korjaamo.ui.MainLayout;
import fi.kurvinen.korjaamo.ui.components.Ilmoitus;
import fi.kurvinen.korjaamo.ui.components.LomakeDialogi;
import fi.kurvinen.korjaamo.ui.components.Rekkari;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Asiakasrekisteri – vain SUPER- ja USER-rooleille (vaatimus 16).
 * <p>
 * 1:N-relaatio (vaatimus 3) näkyy sarakkeessa "Ajoneuvot": jokaisen asiakkaan ajoneuvot
 * rekisterikilven näköisinä merkkeinä.
 */
@Route(value = "asiakkaat", layout = MainLayout.class)
@PageTitle("Asiakkaat | Korjaamo")
@RolesAllowed({"SUPER", "USER"})
@AccessDeniedErrorRouter
public class AsiakasView extends VerticalLayout {

    private final AsiakasService service;
    private final Grid<Asiakas> grid = new Grid<>(Asiakas.class, false);
    private final TextField haku = new TextField();
    private final LomakeDialogi<Asiakas> dialogi = new LomakeDialogi<>(Asiakas.class, "Uusi asiakas", "Muokkaa asiakasta");
    private List<Asiakas> kaikki = List.of();

    public AsiakasView(AsiakasService service) {
        this.service = service;
        setSizeFull();

        haku.setPlaceholder("Hae nimellä, sähköpostilla tai kaupungilla…");
        haku.setPrefixComponent(VaadinIcon.SEARCH.create());
        haku.setValueChangeMode(ValueChangeMode.LAZY);
        haku.setClearButtonVisible(true);
        haku.setWidth("min(420px, 100%)");
        haku.addValueChangeListener(e -> suodata());

        Button uusi = new Button("Uusi asiakas", VaadinIcon.PLUS.create(), e -> dialogi.avaa(null, Asiakas::new));
        uusi.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout ylapalkki = new HorizontalLayout(new H2("Asiakkaat"), haku, uusi);
        ylapalkki.setWidthFull();
        ylapalkki.setAlignItems(FlexComponent.Alignment.BASELINE);
        ylapalkki.expand(ylapalkki.getComponentAt(0));
        ylapalkki.getStyle().set("flex-wrap", "wrap");

        grid.addColumn(Asiakas::getKokoNimi).setHeader("Nimi").setSortable(true).setComparator(Asiakas::getSukunimi).setAutoWidth(true);
        grid.addColumn(Asiakas::getEmail).setHeader("Sähköposti").setAutoWidth(true);
        grid.addColumn(Asiakas::getPuhelin).setHeader("Puhelin").setAutoWidth(true);
        grid.addColumn(a -> a.getPostinumero() == null ? a.getKaupunki() : a.getPostinumero() + " " + a.getKaupunki())
                .setHeader("Paikkakunta").setAutoWidth(true).setSortable(true);
        grid.addComponentColumn(a -> {
            Div d = new Div();
            d.getStyle().set("display", "flex").set("flex-wrap", "wrap").set("gap", "4px");
            a.getAjoneuvot().forEach(aj -> d.add(new Rekkari(aj.getRekisterinumero())));
            return d;
        }).setHeader("Ajoneuvot (1:N)").setWidth("230px").setFlexGrow(1);
        grid.addColumn(a -> a.getMuokattu() == null ? "" : DateTimeFormatter.ofPattern("d.M.yyyy").format(a.getMuokattu())
                + " / " + a.getMuokannut()).setHeader("Muokattu").setAutoWidth(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();
        grid.addItemClickListener(e -> dialogi.avaa(e.getItem(), Asiakas::new));

        luoLomake();
        add(ylapalkki, grid);
        paivita();
    }

    private void luoLomake() {
        TextField etunimi = new TextField("Etunimi");
        TextField sukunimi = new TextField("Sukunimi");
        EmailField email = new EmailField("Sähköposti");
        TextField puhelin = new TextField("Puhelin");
        puhelin.setPlaceholder("+358 40 123 4567");
        TextField katuosoite = new TextField("Katuosoite");
        TextField postinumero = new TextField("Postinumero");
        postinumero.setMaxLength(5);
        TextField kaupunki = new TextField("Kaupunki");

        var b = dialogi.getBinder();
        b.forField(etunimi).bind("etunimi");
        b.forField(sukunimi).bind("sukunimi");
        b.forField(email).bind("email");
        b.forField(puhelin).bind("puhelin");
        b.forField(katuosoite).withNullRepresentation("").bind("katuosoite");
        // Tyhjä postinumero tallennetaan nullina, jolloin @Pattern ei hylkää sitä (kenttä on valinnainen).
        b.forField(postinumero).withNullRepresentation("").bind("postinumero");
        b.forField(kaupunki).bind("kaupunki");
        dialogi.lisaa(etunimi, sukunimi, email, puhelin, katuosoite, postinumero, kaupunki);

        dialogi.kunTallennetaan(a -> {
            service.tallenna(a);
            Ilmoitus.onnistui("Asiakas " + a.getKokoNimi() + " tallennettu");
            paivita();
        });
        dialogi.kunPoistetaan(a -> {
            service.poista(a);
            Ilmoitus.onnistui("Asiakas poistettu");
            paivita();
        });
    }

    private void paivita() {
        kaikki = service.kaikkiAjoneuvoineen();
        suodata();
    }

    private void suodata() {
        String s = haku.getValue().trim().toLowerCase(Locale.ROOT);
        grid.setItems(s.isEmpty() ? kaikki : kaikki.stream()
                .filter(a -> (a.getKokoNimi() + " " + a.getEmail() + " " + a.getKaupunki()).toLowerCase(Locale.ROOT).contains(s))
                .toList());
    }
}
