package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fi.kurvinen.korjaamo.domain.Kayttaja;
import fi.kurvinen.korjaamo.domain.Rooli;
import fi.kurvinen.korjaamo.security.KirjautunutKayttaja;
import fi.kurvinen.korjaamo.service.KayttajaService;
import fi.kurvinen.korjaamo.service.LiiketoimintaVirhe;
import fi.kurvinen.korjaamo.ui.MainLayout;
import fi.kurvinen.korjaamo.ui.components.Ilmoitus;
import fi.kurvinen.korjaamo.ui.components.PoistoVahvistus;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;

/**
 * Käyttäjähallinta – sivu vain ADMIN-roolille (vaatimus 16). Ylläpitäjä voi muuttaa rooleja,
 * estää tunnuksen ja poistaa käyttäjän. Salasanoja ei näytetä eikä voi muokata täältä, koska
 * niistä on tallessa vain tiiviste.
 */
@Route(value = "admin/kayttajat", layout = MainLayout.class)
@PageTitle("Käyttäjähallinta | Korjaamo")
@RolesAllowed("ADMIN")
@AccessDeniedErrorRouter
public class KayttajaHallintaView extends VerticalLayout {

    private final KayttajaService service;
    private final KirjautunutKayttaja kirjautunut;
    private final Grid<Kayttaja> grid = new Grid<>(Kayttaja.class, false);

    public KayttajaHallintaView(KayttajaService service, KirjautunutKayttaja kirjautunut) {
        this.service = service;
        this.kirjautunut = kirjautunut;
        setSizeFull();

        grid.addColumn(Kayttaja::getKayttajatunnus).setHeader("Tunnus").setAutoWidth(true).setSortable(true);
        grid.addColumn(Kayttaja::getNimi).setHeader("Nimi").setAutoWidth(true).setSortable(true);
        grid.addColumn(Kayttaja::getEmail).setHeader("Sähköposti").setAutoWidth(true);
        grid.addComponentColumn(k -> {
            HorizontalLayout h = new HorizontalLayout();
            h.setSpacing(false);
            h.getStyle().set("gap", "4px");
            k.getRoolit().stream().sorted().forEach(r -> {
                Span s = new Span(r.name());
                s.getElement().getThemeList().add(r == Rooli.ADMIN ? "badge error primary" : r == Rooli.SUPER ? "badge primary" : "badge");
                h.add(s);
            });
            return h;
        }).setHeader("Roolit").setAutoWidth(true);
        grid.addComponentColumn(k -> {
            Span s = new Span(k.isAktiivinen() ? "Aktiivinen" : "Estetty");
            s.getElement().getThemeList().add(k.isAktiivinen() ? "badge success" : "badge contrast");
            return s;
        }).setHeader("Tila").setAutoWidth(true);
        grid.addColumn(k -> k.getRekisteroity() == null ? "" : DateTimeFormatter.ofPattern("d.M.yyyy HH:mm").format(k.getRekisteroity()))
                .setHeader("Rekisteröitynyt").setAutoWidth(true);
        grid.addComponentColumn(k -> {
            Button muokkaa = new Button(VaadinIcon.EDIT.create(), e -> muokkaa(k));
            muokkaa.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            muokkaa.setAriaLabel("Muokkaa rooleja");
            Button poista = new Button(VaadinIcon.TRASH.create(), e -> PoistoVahvistus.kysy("käyttäjä " + k.getKayttajatunnus(), () -> {
                try {
                    service.poista(k.getId(), kirjautunut.get().map(Kayttaja::getKayttajatunnus).orElse(""));
                    Ilmoitus.onnistui("Käyttäjä poistettu");
                    paivita();
                } catch (LiiketoimintaVirhe ex) {
                    Ilmoitus.virhe(ex.getMessage());
                }
            }));
            poista.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
            poista.setAriaLabel("Poista käyttäjä");
            return new HorizontalLayout(muokkaa, poista);
        }).setHeader("Toiminnot").setAutoWidth(true).setFlexGrow(0);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        add(new H2("Käyttäjähallinta"),
                new Paragraph("Uudet käyttäjät saavat rekisteröityessään USER-roolin. Ylläpitäjät saavat jokaisesta "
                        + "rekisteröitymisestä sähköposti-ilmoituksen."),
                grid);
        paivita();
    }

    private void muokkaa(Kayttaja k) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Käyttäjä " + k.getKayttajatunnus());
        CheckboxGroup<Rooli> roolit = new CheckboxGroup<>("Roolit", Rooli.values());
        roolit.setValue(k.getRoolit());
        Checkbox aktiivinen = new Checkbox("Tunnus on aktiivinen", k.isAktiivinen());
        d.add(new VerticalLayout(roolit, aktiivinen));
        Button tallenna = new Button("Tallenna", e -> {
            try {
                service.paivitaRoolitJaTila(k.getId(), roolit.getValue(), aktiivinen.getValue());
                Ilmoitus.onnistui("Käyttäjän tiedot päivitetty");
                d.close();
                paivita();
            } catch (LiiketoimintaVirhe ex) {
                Ilmoitus.virhe(ex.getMessage());
            }
        });
        tallenna.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        d.getFooter().add(new Button("Peruuta", e -> d.close()), tallenna);
        d.open();
    }

    private void paivita() {
        grid.setItems(service.kaikki());
    }
}
