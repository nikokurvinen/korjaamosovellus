package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.shared.Registration;
import fi.kurvinen.korjaamo.domain.HuoltoTila;
import fi.kurvinen.korjaamo.domain.Huoltotilaus;
import fi.kurvinen.korjaamo.service.Broadcaster;
import fi.kurvinen.korjaamo.service.HuoltotilausService;
import fi.kurvinen.korjaamo.service.LiiketoimintaVirhe;
import fi.kurvinen.korjaamo.ui.MainLayout;
import fi.kurvinen.korjaamo.ui.components.Ilmoitus;
import fi.kurvinen.korjaamo.ui.components.Rekkari;
import jakarta.annotation.security.PermitAll;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * Korjaamon työjono kanban-tauluna – kaikille kirjautuneille (vaatimus 16: {@code @PermitAll}).
 * <p>
 * Layout-tyyppi (vaatimus 12b): <b>kanban</b>, neljä vierekkäistä saraketta (yksi per
 * {@link HuoltoTila}), jotka rivittyvät kapealla näytöllä allekkain.
 * <p>
 * Vaatimus 8 – näkymäkohtainen CSS: juuriluokka {@code tyojono-view} rajaa tiedoston
 * {@code themes/korjaamoteema/views/tyojono-view.css} säännöt vain tähän näkymään. Tyylit
 * koskevat näkymän H2-, H3-, Button- ja Span-komponentteja sekä sarakkeita ja kortteja.
 * <p>
 * Vaatimus 10: työkortit zoomaavat 3 % hoverissa, saavat ääriviivan focuksessa ja muutokset
 * animoidaan transitionilla. Vaatimus 22: kun joku siirtää tilauksen, kaikkien avoimet työjonot
 * päivittyvät Pushilla ja muuttunut kortti välähtää.
 */
@Route(value = "tyojono", layout = MainLayout.class)
@PageTitle("Työjono | Korjaamo")
@PermitAll
public class TyojonoView extends VerticalLayout {

    private static final DateTimeFormatter PVM = DateTimeFormatter.ofPattern("d.M.yyyy");

    private final HuoltotilausService service;
    private final Broadcaster broadcaster;
    private final FlexLayout sarakkeet = new FlexLayout();
    private final Set<Long> korostettavat = new HashSet<>();
    private Registration rekisterointi;

    public TyojonoView(HuoltotilausService service, Broadcaster broadcaster) {
        this.service = service;
        this.broadcaster = broadcaster;
        addClassName("tyojono-view");
        setSizeFull();
        setPadding(true);

        Button paivita = new Button("Päivitä", VaadinIcon.REFRESH.create(), e -> paivita());
        paivita.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button uusi = new Button("Uusi tilaus", VaadinIcon.PLUS.create(),
                e -> getUI().ifPresent(ui -> ui.navigate(HuoltotilausView.class)));
        uusi.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout otsikkorivi = new HorizontalLayout(new H2("Työjono"), paivita, uusi);
        otsikkorivi.setWidthFull();
        otsikkorivi.setAlignItems(FlexComponent.Alignment.BASELINE);
        otsikkorivi.expand(otsikkorivi.getComponentAt(0));

        Paragraph ohje = new Paragraph("Siirrä tilaus seuraavaan vaiheeseen kortin napista. Muutokset näkyvät "
                + "reaaliajassa kaikille korjaamon käyttäjille.");
        ohje.getStyle().set("margin", "0").set("color", "var(--lumo-secondary-text-color)");

        sarakkeet.setWidthFull();
        sarakkeet.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        sarakkeet.getStyle().set("gap", "var(--lumo-space-m)");
        sarakkeet.setAlignItems(FlexComponent.Alignment.START);
        add(otsikkorivi, ohje, sarakkeet);
        paivita();
    }

    private void paivita() {
        sarakkeet.removeAll();
        for (HuoltoTila tila : HuoltoTila.values()) {
            sarakkeet.add(sarake(tila));
        }
        korostettavat.clear();
    }

    private VerticalLayout sarake(HuoltoTila tila) {
        var tilaukset = service.tilassa(tila);
        Span maara = new Span(String.valueOf(tilaukset.size()));
        maara.addClassName("maara");
        H3 otsikko = new H3(new Span(tila.getNimi()), maara);

        VerticalLayout sarake = new VerticalLayout(otsikko);
        sarake.addClassName("kanban-sarake");
        sarake.getStyle().set("--sarakkeen-vari", tila.getVari());
        sarake.setSpacing(true);
        if (tilaukset.isEmpty()) {
            Span tyhja = new Span("Ei tilauksia");
            tyhja.getStyle().set("color", "var(--lumo-tertiary-text-color)");
            sarake.add(tyhja);
        }
        tilaukset.forEach(h -> sarake.add(kortti(h)));
        return sarake;
    }

    private Div kortti(Huoltotilaus h) {
        Span otsikko = new Span(h.getOtsikko());
        otsikko.getStyle().set("font-weight", "600").set("display", "block");
        Span omistaja = new Span(h.getAjoneuvo().getMerkki() + " " + h.getAjoneuvo().getMalli()
                + " · " + h.getAjoneuvo().getOmistaja().getKokoNimi());
        omistaja.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)")
                .set("display", "block");
        Span pvm = new Span("Vastaanotettu " + PVM.format(h.getVastaanotettu())
                + (h.getValmistuu() != null ? " · valmis " + PVM.format(h.getValmistuu()) : ""));
        pvm.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("color", "var(--lumo-tertiary-text-color)");

        Div kortti = new Div(new Rekkari(h.getAjoneuvo().getRekisterinumero()), otsikko, omistaja, pvm);
        kortti.addClassName("tyokortti");
        if (korostettavat.contains(h.getId())) {
            kortti.addClassName("juuri-paivitetty");
        }
        kortti.getElement().setAttribute("tabindex", "0");

        HorizontalLayout napit = new HorizontalLayout();
        napit.setSpacing(false);
        napit.getStyle().set("gap", "var(--lumo-space-xs)").set("flex-wrap", "wrap");
        HuoltoTila seuraava = h.getTila().seuraava();
        if (seuraava != null) {
            Button siirra = new Button("→ " + seuraava.getNimi(), e -> siirra(h));
            siirra.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            napit.add(siirra);
        }
        Button avaa = new Button("Avaa", VaadinIcon.EDIT.create(), e -> getUI().ifPresent(ui ->
                ui.navigate(HuoltotilausView.class, new RouteParameters("id", String.valueOf(h.getId())))));
        avaa.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        napit.add(avaa);
        kortti.add(napit);
        return kortti;
    }

    private void siirra(Huoltotilaus h) {
        try {
            Huoltotilaus paivitetty = service.siirraSeuraavaan(h.getId());
            Ilmoitus.onnistui(h.getAjoneuvo().getRekisterinumero() + " → " + paivitetty.getTila().getNimi());
        } catch (LiiketoimintaVirhe e) {
            Ilmoitus.virhe(e.getMessage());
            paivita();
        }
        // Näkymä päivittyy Broadcasterin kautta samalla tavalla kuin muiden käyttäjien näkymät.
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        rekisterointi = broadcaster.rekisteroi(t -> {
            if (!"Huoltotilaus".equals(t.kohde())) return;
            ui.access(() -> {
                if (t.kohdeId() != null) korostettavat.add(t.kohdeId());
                paivita();
            });
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (rekisterointi != null) {
            rekisterointi.remove();
        }
    }
}
