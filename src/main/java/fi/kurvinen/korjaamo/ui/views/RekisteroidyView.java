package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import fi.kurvinen.korjaamo.service.KayttajaService;
import fi.kurvinen.korjaamo.service.LiiketoimintaVirhe;
import fi.kurvinen.korjaamo.ui.MainLayout;
import fi.kurvinen.korjaamo.ui.components.Ilmoitus;

/**
 * Rekisteröityminen (vaatimus 17). Uusi käyttäjä saa aina USER-roolin, ja ylläpitäjät saavat
 * sähköposti-ilmoituksen (vaatimus 26). Sivu on lokalisoitu (vaatimus 23).
 * <p>
 * Kenttien perusmuoto tarkistetaan jo selaimessa (pattern, minLength), mutta varsinainen validointi
 * ja yksilöllisyystarkistus tehdään palvelimella {@link KayttajaService#rekisteroi}-metodissa.
 */
@Route(value = "rekisteroidy", layout = MainLayout.class)
@AnonymousAllowed
public class RekisteroidyView extends VerticalLayout implements LocaleChangeObserver, HasDynamicTitle {

    private final H2 otsikko = new H2();
    private final Paragraph kuvaus = new Paragraph();
    private final TextField tunnus = new TextField();
    private final TextField nimi = new TextField();
    private final EmailField email = new EmailField();
    private final PasswordField salasana = new PasswordField();
    private final PasswordField salasana2 = new PasswordField();
    private final Button laheta = new Button();
    private final RouterLink kirjautumaan = new RouterLink("", LoginView.class);

    public RekisteroidyView(KayttajaService kayttajaService) {
        addClassName("auth-view");
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        tunnus.setRequired(true);
        tunnus.setPattern("[a-z0-9_.]{3,30}");
        tunnus.setMaxLength(30);
        nimi.setRequired(true);
        email.setRequiredIndicatorVisible(true);
        salasana.setRequired(true);
        salasana.setMinLength(KayttajaService.SALASANAN_MIN_PITUUS);
        salasana2.setRequired(true);

        FormLayout lomake = new FormLayout(tunnus, nimi, email, salasana, salasana2);
        lomake.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        laheta.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        laheta.setWidthFull();
        laheta.addClickListener(e -> {
            if (!salasana.getValue().equals(salasana2.getValue())) {
                salasana2.setInvalid(true);
                salasana2.setErrorMessage(getTranslation("rekisteroidy.virhe.eriSalasanat"));
                return;
            }
            try {
                kayttajaService.rekisteroi(tunnus.getValue(), nimi.getValue(), email.getValue(), salasana.getValue());
                Ilmoitus.onnistui(getTranslation("rekisteroidy.onnistui"));
                getUI().ifPresent(ui -> ui.navigate(LoginView.class));
            } catch (LiiketoimintaVirhe ex) {
                Ilmoitus.virhe(ex.getMessage());
            }
        });

        VerticalLayout kortti = new VerticalLayout(otsikko, kuvaus, lomake, laheta, kirjautumaan);
        kortti.addClassName("auth-kortti");
        kortti.setWidth("min(440px, 100%)");
        kortti.setPadding(false);
        add(kortti);
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        otsikko.setText(getTranslation("rekisteroidy.otsikko"));
        kuvaus.setText(getTranslation("rekisteroidy.kuvaus"));
        tunnus.setLabel(getTranslation("rekisteroidy.tunnus"));
        tunnus.setHelperText(getTranslation("rekisteroidy.tunnus.ohje"));
        nimi.setLabel(getTranslation("rekisteroidy.nimi"));
        email.setLabel(getTranslation("rekisteroidy.email"));
        email.setErrorMessage(getTranslation("rekisteroidy.virhe.email"));
        salasana.setLabel(getTranslation("rekisteroidy.salasana"));
        salasana.setHelperText(getTranslation("rekisteroidy.salasana.ohje"));
        salasana2.setLabel(getTranslation("rekisteroidy.salasana2"));
        laheta.setText(getTranslation("rekisteroidy.laheta"));
        kirjautumaan.setText(getTranslation("rekisteroidy.onTunnus"));
    }

    @Override
    public String getPageTitle() {
        return getTranslation("rekisteroidy.otsikko") + " | Korjaamo";
    }
}
