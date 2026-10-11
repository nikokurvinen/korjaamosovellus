package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import fi.kurvinen.korjaamo.ui.MainLayout;

/**
 * Kirjautuminen (vaatimus 16). Vaadinin {@link LoginForm} lähettää lomakkeen Spring Securityn
 * {@code /login}-osoitteeseen, joten salasanan tarkistus tapahtuu kokonaan Spring Securityssä.
 * Sivu on lokalisoitu (vaatimus 23).
 */
@Route(value = "login", layout = MainLayout.class)
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver, LocaleChangeObserver, HasDynamicTitle {

    private final LoginForm login = new LoginForm();
    private final H2 otsikko = new H2();
    private final Paragraph ohje = new Paragraph();
    private final Button rekisteroidy = new Button();
    private final Button unohtui = new Button();

    public LoginView() {
        addClassName("auth-view");
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);

        rekisteroidy.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        rekisteroidy.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(RekisteroidyView.class)));
        unohtui.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        unohtui.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(SalasanaUnohtuiView.class)));

        ohje.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-s)");

        VerticalLayout kortti = new VerticalLayout(otsikko, login, new HorizontalLayout(rekisteroidy, unohtui), ohje);
        kortti.addClassName("auth-kortti");
        kortti.setWidth("min(440px, 100%)");
        kortti.setAlignItems(FlexComponent.Alignment.CENTER);
        kortti.setPadding(false);
        add(kortti);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Spring Security ohjaa epäonnistuneen kirjautumisen osoitteeseen /login?error
        login.setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        otsikko.setText(getTranslation("login.otsikko"));
        ohje.setText(getTranslation("login.demotunnukset"));
        rekisteroidy.setText(getTranslation("login.rekisteroidy"));
        unohtui.setText(getTranslation("login.unohtui"));

        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle("");
        i18n.getForm().setUsername(getTranslation("login.kayttajatunnus"));
        i18n.getForm().setPassword(getTranslation("login.salasana"));
        i18n.getForm().setSubmit(getTranslation("login.kirjaudu"));
        i18n.getErrorMessage().setTitle(getTranslation("login.virhe.otsikko"));
        i18n.getErrorMessage().setMessage(getTranslation("login.virhe.teksti"));
        login.setI18n(i18n);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("login.otsikko") + " | Korjaamo";
    }
}
