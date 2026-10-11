package fi.kurvinen.korjaamo.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.theme.lumo.LumoUtility;
import fi.kurvinen.korjaamo.domain.Kayttaja;
import fi.kurvinen.korjaamo.security.KirjautunutKayttaja;
import fi.kurvinen.korjaamo.ui.components.AppFooter;
import fi.kurvinen.korjaamo.ui.views.AjoneuvoView;
import fi.kurvinen.korjaamo.ui.views.AsiakasView;
import fi.kurvinen.korjaamo.ui.views.DashboardView;
import fi.kurvinen.korjaamo.ui.views.HakuView;
import fi.kurvinen.korjaamo.ui.views.HistoriaView;
import fi.kurvinen.korjaamo.ui.views.HuoltotilausView;
import fi.kurvinen.korjaamo.ui.views.KayttajaHallintaView;
import fi.kurvinen.korjaamo.ui.views.LoginView;
import fi.kurvinen.korjaamo.ui.views.ProfiiliView;
import fi.kurvinen.korjaamo.ui.views.TyojonoView;
import fi.kurvinen.korjaamo.ui.views.VakuutusView;
import fi.kurvinen.korjaamo.ui.views.VaraosaView;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Sovelluksen SPA-runko (vaatimukset 11, 13, 14, 15).
 * <p>
 * Kaikki näkymät määritellään muodossa {@code @Route(value = "...", layout = MainLayout.class)}, jolloin
 * Vaadin vaihtaa navigoidessa vain sisältöalueen – header, navigaatio ja footer pysyvät paikallaan
 * eikä sivua ladata uudelleen.
 * <p>
 * Footer: AppLayoutissa ei ole footer-slottia. Siksi {@link #showRouterLayoutContent} on ylikirjoitettu
 * niin, että näkymä sijoitetaan kääreen sisään ja footer sen perään. Kääre on flex-sarake, jonka
 * sisältöosa kasvaa ({@code flex: 1}), joten footer pysyy näkymän alareunassa myös lyhyillä sivuilla.
 */
public class MainLayout extends AppLayout implements LocaleChangeObserver {

    public static final Locale SUOMI = Locale.of("fi", "FI");
    public static final Locale ENGLANTI = Locale.ENGLISH;

    private final KirjautunutKayttaja kirjautunut;
    private final AccessAnnotationChecker accessChecker;

    private final Div sisalto = new Div();
    private final Scroller navScroller = new Scroller();
    private final HorizontalLayout kayttajaAlue = new HorizontalLayout();
    private final Select<Locale> kielivalinta = new Select<>();

    public MainLayout(KirjautunutKayttaja kirjautunut, AccessAnnotationChecker accessChecker) {
        this.kirjautunut = kirjautunut;
        this.accessChecker = accessChecker;
        setPrimarySection(Section.DRAWER);

        Div kaare = new Div();
        kaare.addClassName("content-wrapper");
        sisalto.addClassName("view-content");
        kaare.add(sisalto, new AppFooter());
        setContent(kaare);

        luoHeader();
        luoDrawer();
    }

    // ------------------------------------------------------------------ Header (vaatimus 13)

    private void luoHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Valikko");

        kielivalinta.setItems(SUOMI, ENGLANTI);
        kielivalinta.setItemLabelGenerator(l -> l.getLanguage().equals("fi") ? "FI" : "EN");
        kielivalinta.setWidth("5.5em");
        kielivalinta.addClassName("kielivalinta");
        kielivalinta.setAriaLabel("Kieli / Language");
        kielivalinta.addValueChangeListener(e -> {
            if (e.isFromClient() && e.getValue() != null) {
                // Session locale päivittää kaikki saman istunnon UI:t ja tallentuu navigoinnin yli.
                VaadinSession.getCurrent().setLocale(e.getValue());
            }
        });

        kayttajaAlue.setAlignItems(FlexComponent.Alignment.CENTER);
        kayttajaAlue.setSpacing(false);
        kayttajaAlue.getStyle().set("gap", "var(--lumo-space-xs)");

        RouterLink logo = new RouterLink();
        logo.setRoute(DashboardView.class);
        logo.add(luoLogo());
        logo.getStyle().set("text-decoration", "none");

        Div valitila = new Div();
        HorizontalLayout header = new HorizontalLayout(toggle, logo, valitila, kielivalinta, kayttajaAlue);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(valitila);
        header.addClassNames(LumoUtility.Padding.End.MEDIUM, LumoUtility.Gap.SMALL);
        addToNavbar(header);
    }

    private void paivitaKayttajaAlue() {
        kayttajaAlue.removeAll();
        Optional<Kayttaja> kayttaja = kirjautunut.get();
        if (kayttaja.isPresent()) {
            Kayttaja k = kayttaja.get();
            Avatar avatar = new Avatar(k.getNimi());
            if (k.getProfiilikuva() != null) {
                byte[] kuva = k.getProfiilikuva();
                String tyyppi = k.getProfiilikuvanTyyppi();
                avatar.setImageHandler(DownloadHandler.fromInputStream(ev ->
                        new DownloadResponse(new ByteArrayInputStream(kuva), "avatar", tyyppi, kuva.length)));
            }
            avatar.addClassName("header-avatar");
            avatar.getElement().setAttribute("title", k.getNimi());

            Span nimi = new Span(k.getNimi() + " (" + String.join(", ", k.getRoolit().stream().map(Enum::name).sorted().toList()) + ")");
            nimi.addClassName("app-header-user");

            RouterLink profiili = new RouterLink();
            profiili.setRoute(ProfiiliView.class);
            profiili.add(avatar);

            Button ulos = new Button(getTranslation("header.kirjauduUlos"), VaadinIcon.SIGN_OUT.create(),
                    e -> kirjautunut.kirjauduUlos());
            ulos.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            ulos.addClassName("header-nappi");
            kayttajaAlue.add(nimi, profiili, ulos);
        } else {
            Button kirjaudu = new Button(getTranslation("header.kirjaudu"), VaadinIcon.SIGN_IN.create(),
                    e -> UI.getCurrent().navigate(LoginView.class));
            kirjaudu.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            kayttajaAlue.add(kirjaudu);
        }
    }

    // ------------------------------------------------------------------ Navigaatio (vaatimus 14)

    /** Sovelluksen nimi ja logo headerissa (vaatimus 13): jakoavain-ikoni + "KORJAAMO·PRO". */
    private H1 luoLogo() {
        H1 logo = new H1();
        logo.addClassName("app-logo");
        var ikoni = VaadinIcon.WRENCH.create();
        ikoni.addClassName("app-logo-ikoni");
        Span pro = new Span("Pro");
        logo.add(ikoni, new Span("Korjaamo"), pro);
        return logo;
    }

    private void luoDrawer() {
        navScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        navScroller.addClassNames(LumoUtility.Padding.Top.SMALL);
        addToDrawer(navScroller);
    }

    private void paivitaNavigaatio() {
        Div navit = new Div();
        navit.add(osio("nav.osio.tyo", Arrays.asList(
                kohde("nav.etusivu", DashboardView.class, VaadinIcon.DASHBOARD),
                kohde("nav.tyojono", TyojonoView.class, VaadinIcon.TOOLBOX),
                kohde("nav.huollot", HuoltotilausView.class, VaadinIcon.WRENCH),
                kohde("nav.haku", HakuView.class, VaadinIcon.SEARCH))));
        navit.add(osio("nav.osio.rekisterit", Arrays.asList(
                kohde("nav.asiakkaat", AsiakasView.class, VaadinIcon.USERS),
                kohde("nav.ajoneuvot", AjoneuvoView.class, VaadinIcon.CAR),
                kohde("nav.vakuutukset", VakuutusView.class, VaadinIcon.SHIELD),
                kohde("nav.varaosat", VaraosaView.class, VaadinIcon.COGS))));
        navit.add(osio("nav.osio.yllapito", Arrays.asList(
                kohde("nav.kayttajat", KayttajaHallintaView.class, VaadinIcon.USER_STAR),
                kohde("nav.historia", HistoriaView.class, VaadinIcon.TIME_BACKWARD))));
        navit.add(osio("nav.osio.oma", Arrays.asList(
                kohde("nav.profiili", ProfiiliView.class, VaadinIcon.USER_CARD))));
        navScroller.setContent(navit);
    }

    /**
     * Navigaatiokohde lisätään vain, jos käyttäjällä on oikeus näkymään. Tarkistus lukee samat
     * annotaatiot (@RolesAllowed jne.) kuin Vaadinin reitityksen pääsynhallinta, joten valikko ja
     * todelliset oikeudet eivät voi joutua ristiriitaan.
     */
    private SideNavItem kohde(String avain, Class<? extends Component> nakyma, VaadinIcon ikoni) {
        if (!accessChecker.hasAccess(nakyma)) {
            return null;
        }
        return new SideNavItem(getTranslation(avain), nakyma, ikoni.create());
    }

    private Component osio(String otsikkoAvain, List<SideNavItem> kohteet) {
        SideNav nav = new SideNav();
        kohteet.stream().filter(k -> k != null).forEach(nav::addItem);
        if (nav.getItems().isEmpty()) {
            return new Div();
        }
        Span otsikko = new Span(getTranslation(otsikkoAvain));
        otsikko.addClassName("nav-section-title");
        nav.setWidthFull();
        nav.addClassNames(LumoUtility.Padding.Horizontal.SMALL);
        return new Div(otsikko, nav);
    }

    // ------------------------------------------------------------------ Sisältö + footer

    @Override
    public void showRouterLayoutContent(HasElement content) {
        sisalto.removeAll();
        if (content != null) {
            sisalto.getElement().appendChild(content.getElement());
        }
    }

    @Override
    public void removeRouterLayoutContent(HasElement oldContent) {
        sisalto.getElement().removeAllChildren();
    }

    // ------------------------------------------------------------------ Lokalisointi (vaatimus 23)

    @Override
    public void localeChange(LocaleChangeEvent event) {
        kielivalinta.setValue(event.getLocale().getLanguage().equals("en") ? ENGLANTI : SUOMI);
        paivitaKayttajaAlue();
        paivitaNavigaatio();
    }
}
