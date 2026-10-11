package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility.Background;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderRadius;
import com.vaadin.flow.theme.lumo.LumoUtility.BoxShadow;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexDirection;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.TextColor;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;
import fi.kurvinen.korjaamo.domain.HuoltoTila;
import fi.kurvinen.korjaamo.security.KirjautunutKayttaja;
import fi.kurvinen.korjaamo.service.Broadcaster;
import fi.kurvinen.korjaamo.service.HuoltotilausService;
import fi.kurvinen.korjaamo.service.HuoltotilausService.Kuukausitilasto;
import fi.kurvinen.korjaamo.service.Kello;
import fi.kurvinen.korjaamo.service.MuutosTapahtuma;
import fi.kurvinen.korjaamo.ui.MainLayout;
import fi.kurvinen.korjaamo.ui.components.HuoltoKaavio;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Etusivu / dashboard – näkyy kaikille, myös kirjautumattomille (vaatimus 16).
 * <p>
 * Layout-tyyppi (vaatimus 12a): <b>korttiruudukko</b>. Rivit ovat rivittyviä FlexLayouteja, joissa
 * on tilastokortit, kaksi Chart.js-kaaviota (vaatimus 33) ja live-muutoslista.
 * <p>
 * Vaatimus 9 – Lumo Utility -luokat. Näkymässä käytetään: Background, TextColor, Padding, BoxShadow,
 * BorderRadius, Display, FontSize, Margin, Width ja Gap. Utility-luokat sopivat juuri tähän
 * näkymään, koska kortteja on paljon ja niiden ulkoasu koostuu pienistä, toistuvista määrityksistä.
 * <p>
 * Vaatimus 22 – Server Push: kello päivittyy sekunnin välein ja live-muutokset, tilastot ja kaaviot
 * päivittyvät heti, kun kuka tahansa tallentaa muutoksen.
 */
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "etusivu", layout = MainLayout.class)
@AnonymousAllowed
public class DashboardView extends Div implements LocaleChangeObserver, HasDynamicTitle {

    private static final DateTimeFormatter KELLO = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final HuoltotilausService huoltoService;
    private final Broadcaster broadcaster;
    private final Kello kello;
    private final KirjautunutKayttaja kirjautunut;

    private final H1 otsikko = new H1();
    private final Paragraph alaotsikko = new Paragraph();
    private final Span kelloTeksti = new Span();
    private final FlexLayout kortit = new FlexLayout();
    private final HuoltoKaavio tilaKaavio = new HuoltoKaavio();
    private final HuoltoKaavio kuukausiKaavio = new HuoltoKaavio();
    private final H3 tilaOtsikko = new H3();
    private final H3 kuukausiOtsikko = new H3();
    private final H3 liveOtsikko = new H3();
    private final Div liveLista = new Div();
    private final Div toimintoAlue = new Div();

    private Registration broadcastRekisterointi;
    private Registration kelloRekisterointi;

    public DashboardView(HuoltotilausService huoltoService, Broadcaster broadcaster, Kello kello,
                         KirjautunutKayttaja kirjautunut) {
        this.huoltoService = huoltoService;
        this.broadcaster = broadcaster;
        this.kello = kello;
        this.kirjautunut = kirjautunut;
        addClassNames(Padding.LARGE, Display.FLEX, FlexDirection.COLUMN, Gap.LARGE);

        add(luoBanneri(), kortit, luoKaaviorivi());
        kortit.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        kortit.addClassNames(Gap.MEDIUM);

        tilaKaavio.addSegmenttiKlikattuListener(e -> Arrays.stream(HuoltoTila.values())
                .filter(t -> getTranslation("tila." + t.name()).equals(e.getOtsikko()))
                .findFirst()
                .ifPresent(this::avaaHaku));
    }

    /** Tumma banneri: otsikko, tervehdys, live-kello ja toimintonapit. */
    private Div luoBanneri() {
        otsikko.addClassNames(Margin.NONE, TextColor.PRIMARY_CONTRAST, FontSize.XXXLARGE);
        alaotsikko.addClassNames(Margin.Top.XSMALL, Margin.Bottom.NONE, FontSize.LARGE);
        alaotsikko.getStyle().set("color", "hsla(0,0%,100%,0.8)");

        Icon kelloIkoni = VaadinIcon.CLOCK.create();
        kelloIkoni.setSize("var(--lumo-icon-size-s)");
        kelloTeksti.addClassNames(FontSize.XLARGE, FontWeight.SEMIBOLD);
        Span kelloAlue = new Span(kelloIkoni, kelloTeksti);
        kelloAlue.addClassNames(Display.FLEX, Gap.SMALL, Padding.Horizontal.MEDIUM, Padding.Vertical.SMALL,
                BorderRadius.MEDIUM, TextColor.PRIMARY_CONTRAST);
        kelloAlue.getStyle().set("align-items", "center").set("background", "hsla(0,0%,100%,0.1)")
                .set("font-variant-numeric", "tabular-nums");

        Div tekstit = new Div(otsikko, alaotsikko, toimintoAlue);
        toimintoAlue.addClassNames(Display.FLEX, Gap.SMALL, Margin.Top.MEDIUM);

        Div banneri = new Div(tekstit, kelloAlue);
        banneri.addClassNames(Display.FLEX, Padding.LARGE, BorderRadius.LARGE, BoxShadow.MEDIUM, Gap.LARGE);
        banneri.getStyle()
                .set("background", "linear-gradient(120deg, var(--korjaamo-grafiitti) 0%, var(--korjaamo-grafiitti-2) 60%, var(--korjaamo-oranssi-tumma) 140%)")
                .set("justify-content", "space-between")
                .set("align-items", "flex-start")
                .set("flex-wrap", "wrap");
        return banneri;
    }

    private FlexLayout luoKaaviorivi() {
        Div tila = kaaviokortti(tilaOtsikko, tilaKaavio);
        Div kk = kaaviokortti(kuukausiOtsikko, kuukausiKaavio);
        kk.getStyle().set("flex", "2 1 420px");

        liveOtsikko.addClassNames(Margin.Top.NONE, FontSize.LARGE);
        liveLista.addClassNames(Display.FLEX, FlexDirection.COLUMN, Gap.SMALL);
        Div live = new Div(liveOtsikko, liveLista);
        live.addClassNames(Background.BASE, Padding.MEDIUM, BorderRadius.LARGE, BoxShadow.SMALL);
        live.getStyle().set("flex", "1 1 280px");

        FlexLayout rivi = new FlexLayout(tila, kk, live);
        rivi.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        rivi.addClassNames(Gap.MEDIUM);
        return rivi;
    }

    private Div kaaviokortti(H3 otsikko, HuoltoKaavio kaavio) {
        otsikko.addClassNames(Margin.Top.NONE, Margin.Bottom.SMALL, FontSize.LARGE);
        Div kortti = new Div(otsikko, kaavio);
        kortti.addClassNames(Background.BASE, Padding.MEDIUM, BorderRadius.LARGE, BoxShadow.SMALL);
        kortti.getStyle().set("flex", "1 1 300px");
        return kortti;
    }

    private void paivitaTilastot() {
        Map<HuoltoTila, Long> maarat = huoltoService.maaratTiloittain();
        kortit.removeAll();
        VaadinIcon[] ikonit = {VaadinIcon.INBOX, VaadinIcon.WRENCH, VaadinIcon.CHECK_CIRCLE, VaadinIcon.CAR};
        for (HuoltoTila t : HuoltoTila.values()) {
            kortit.add(tilastokortti(t, maarat.get(t), ikonit[t.ordinal()]));
        }

        tilaKaavio.naytaDonitsi(
                Arrays.stream(HuoltoTila.values()).map(t -> getTranslation("tila." + t.name())).toList(),
                Arrays.stream(HuoltoTila.values()).map(maarat::get).toList(),
                List.of("hsl(205,35%,45%)", "hsl(24,95%,50%)", "hsl(150,55%,36%)", "hsl(220,8%,60%)"));

        Map<YearMonth, Kuukausitilasto> kk = huoltoService.kuukausitilastot();
        List<String> kuukaudet = kk.keySet().stream()
                .map(ym -> ym.getMonth().getDisplayName(TextStyle.SHORT, getLocale()) + " " + (ym.getYear() % 100))
                .toList();
        kuukausiKaavio.naytaPylvasJaViiva(kuukaudet,
                getTranslation("dashboard.kaavio.maara"), kk.values().stream().map(Kuukausitilasto::maara).toList(),
                getTranslation("dashboard.kaavio.liikevaihto"), kk.values().stream().map(Kuukausitilasto::liikevaihto).map(BigDecimal::intValue).toList());
    }

    /**
     * Tilastokortti: hover nostaa korttia ja focus näyttää ääriviivan (teeman .tilastokortti,
     * vaatimus 10). tabindex=0 tekee kortista näppäimistöllä fokusoitavan, ja Enter avaa haun.
     */
    private Div tilastokortti(HuoltoTila tila, long maara, VaadinIcon ikoni) {
        Icon i = ikoni.create();
        i.setSize("28px");
        i.getStyle().set("color", tila.getVari());
        Span luku = new Span(String.valueOf(maara));
        luku.addClassNames(FontSize.XXXLARGE, FontWeight.BOLD, TextColor.HEADER);
        Span nimi = new Span(getTranslation("tila." + tila.name()));
        nimi.addClassNames(FontSize.SMALL, TextColor.SECONDARY);

        Div kortti = new Div(i, luku, nimi);
        kortti.addClassName("tilastokortti");
        kortti.addClassNames(Background.BASE, Padding.MEDIUM, BorderRadius.LARGE, BoxShadow.SMALL,
                Display.FLEX, FlexDirection.COLUMN, Gap.XSMALL, Width.FULL);
        kortti.getStyle().set("border-top-color", tila.getVari()).set("flex", "1 1 180px").set("max-width", "none");
        kortti.getElement().setAttribute("tabindex", "0");
        kortti.getElement().setAttribute("role", "button");
        kortti.getElement().addEventListener("click", e -> avaaHaku(tila));
        kortti.getElement().addEventListener("keydown", e -> avaaHaku(tila)).setFilter("event.key === 'Enter'");
        return kortti;
    }

    private void avaaHaku(HuoltoTila tila) {
        if (kirjautunut.onKirjautunut()) {
            getUI().ifPresent(ui -> ui.navigate(HakuView.class, QueryParameters.of("tila", tila.name())));
        }
    }

    private void paivitaLiveLista() {
        liveLista.removeAll();
        List<MuutosTapahtuma> tapahtumat = broadcaster.viimeisimmat();
        if (tapahtumat.isEmpty()) {
            Span tyhja = new Span(getTranslation("dashboard.live.tyhja"));
            tyhja.addClassNames(TextColor.SECONDARY, FontSize.SMALL);
            liveLista.add(tyhja);
        }
        for (MuutosTapahtuma t : tapahtumat) {
            Span aika = new Span(KELLO.format(t.aika()));
            aika.addClassNames(FontSize.XSMALL, TextColor.SECONDARY);
            Span teksti = new Span(t.kohde() + " " + t.toiminto() + ": " + t.kuvaus());
            teksti.addClassNames(FontSize.SMALL, TextColor.BODY);
            Span tekija = new Span("– " + t.tekija());
            tekija.addClassNames(FontSize.XSMALL, TextColor.TERTIARY);
            Div rivi = new Div(aika, teksti, tekija);
            rivi.addClassNames(Display.FLEX, FlexDirection.COLUMN, Padding.SMALL, BorderRadius.MEDIUM, Background.CONTRAST_5);
            liveLista.add(rivi);
        }
    }

    private void paivitaToiminnot() {
        toimintoAlue.removeAll();
        if (kirjautunut.onKirjautunut()) {
            Button jono = new Button(getTranslation("dashboard.avaaTyojono"), VaadinIcon.TOOLBOX.create(),
                    e -> getUI().ifPresent(ui -> ui.navigate(TyojonoView.class)));
            jono.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            jono.addClassName("fade-in-nappi");
            Button uusi = new Button(getTranslation("dashboard.uusiTilaus"), VaadinIcon.PLUS.create(),
                    e -> getUI().ifPresent(ui -> ui.navigate(HuoltotilausView.class)));
            uusi.addClassName("fade-in-nappi");
            uusi.getStyle().set("animation-delay", "0.15s");
            toimintoAlue.add(jono, uusi);
        } else {
            Button kirjaudu = new Button(getTranslation("header.kirjaudu"), VaadinIcon.SIGN_IN.create(),
                    e -> getUI().ifPresent(ui -> ui.navigate(LoginView.class)));
            kirjaudu.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            kirjaudu.addClassName("fade-in-nappi");
            Button rek = new Button(getTranslation("login.rekisteroidy"),
                    e -> getUI().ifPresent(ui -> ui.navigate(RekisteroidyView.class)));
            rek.addClassName("fade-in-nappi");
            rek.getStyle().set("animation-delay", "0.15s");
            toimintoAlue.add(kirjaudu, rek);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        kelloTeksti.setText(KELLO.format(java.time.LocalTime.now()));
        kelloRekisterointi = kello.rekisteroi(aika -> ui.access(() -> kelloTeksti.setText(KELLO.format(aika))));
        // ui.access lukitsee istunnon ja lähettää muutokset Pushilla heti, kun lukko vapautuu.
        broadcastRekisterointi = broadcaster.rekisteroi(t -> ui.access(() -> {
            paivitaTilastot();
            paivitaLiveLista();
        }));
    }

    /**
     * Rekisteröinnit on pakko poistaa: muuten Broadcaster ja Kello pitäisivät viitettä suljettuun
     * näkymään (ja koko sen UI:hin) ikuisesti – muistivuoto, joka kasvaa jokaisella sivulatauksella.
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (broadcastRekisterointi != null) broadcastRekisterointi.remove();
        if (kelloRekisterointi != null) kelloRekisterointi.remove();
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        otsikko.setText(getTranslation("dashboard.otsikko"));
        alaotsikko.setText(kirjautunut.get()
                .map(k -> getTranslation("dashboard.tervehdys", k.getNimi()))
                .orElse(getTranslation("dashboard.vierailija")));
        tilaOtsikko.setText(getTranslation("dashboard.kaavio.tilat"));
        kuukausiOtsikko.setText(getTranslation("dashboard.kaavio.kuukaudet"));
        liveOtsikko.setText(getTranslation("dashboard.live.otsikko"));
        paivitaToiminnot();
        paivitaTilastot();
        paivitaLiveLista();
    }

    @Override
    public String getPageTitle() {
        return getTranslation("nav.etusivu") + " | Korjaamo";
    }
}
