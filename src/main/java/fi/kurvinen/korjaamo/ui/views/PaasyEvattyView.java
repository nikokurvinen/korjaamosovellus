package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import fi.kurvinen.korjaamo.security.KirjautunutKayttaja;
import fi.kurvinen.korjaamo.ui.MainLayout;

/**
 * Oma virhesivu, kun käyttöoikeudet eivät riitä (vaatimus 18).
 * <p>
 * Vaadin heittää {@link AccessDeniedException}in, kun kirjautunut käyttäjä yrittää näkymään, jonka
 * {@code @RolesAllowed} ei salli hänen rooliaan. {@link HasErrorParameter} rekisteröi tämän luokan
 * kyseisen poikkeuksen käsittelijäksi. Tuotantotilassa Vaadin muuntaa pääsyn eston oletuksena
 * 404-virheeksi (ettei suojattujen sivujen olemassaolo paljastu); siksi suojatuissa näkymissä on
 * {@code @AccessDeniedErrorRouter}, joka ohjaa tänne.
 */
@ParentLayout(MainLayout.class)
@PageTitle("Pääsy evätty | Korjaamo")
@AnonymousAllowed
public class PaasyEvattyView extends VerticalLayout implements HasErrorParameter<AccessDeniedException> {

    private final Paragraph selitys = new Paragraph();

    public PaasyEvattyView(KirjautunutKayttaja kirjautunut) {
        addClassName("virhesivu");
        setAlignItems(Alignment.CENTER);

        H2 koodi = new H2("403");
        koodi.addClassName("virhekoodi");
        var ikoni = VaadinIcon.LOCK.create();
        ikoni.setSize("64px");
        ikoni.getStyle().set("color", "var(--korjaamo-teras)");

        Button etusivulle = new Button("Takaisin etusivulle", VaadinIcon.HOME.create(),
                e -> getUI().ifPresent(ui -> ui.navigate(DashboardView.class)));
        etusivulle.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        String rooli = kirjautunut.get()
                .map(k -> "Olet kirjautunut käyttäjänä " + k.getKayttajatunnus() + " (roolit: "
                        + String.join(", ", k.getRoolit().stream().map(Enum::name).toList()) + ").")
                .orElse("");
        add(ikoni, koodi, new H2("Tähän korjaamon osaan ei ole pääsyä"), selitys, new Paragraph(rooli), etusivulle);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        selitys.setText("Sivu /" + event.getLocation().getPath()
                + " vaatii käyttöoikeuden, jota sinulla ei ole. Jos tarvitset pääsyn, ota yhteyttä ylläpitäjään.");
        return HttpStatusCode.FORBIDDEN.getCode();
    }
}
