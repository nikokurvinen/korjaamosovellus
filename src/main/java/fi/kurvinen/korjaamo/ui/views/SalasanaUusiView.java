package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import fi.kurvinen.korjaamo.service.KayttajaService;
import fi.kurvinen.korjaamo.service.LiiketoimintaVirhe;
import fi.kurvinen.korjaamo.service.SalasananPalautusService;
import fi.kurvinen.korjaamo.ui.MainLayout;
import fi.kurvinen.korjaamo.ui.components.Ilmoitus;

/** Uuden salasanan asettaminen sähköpostin linkillä (vaatimus 27). */
@Route(value = "salasana/uusi", layout = MainLayout.class)
@PageTitle("Uusi salasana | Korjaamo")
@AnonymousAllowed
public class SalasanaUusiView extends VerticalLayout implements BeforeEnterObserver {

    private final SalasananPalautusService palautusService;
    private final VerticalLayout kortti = new VerticalLayout();
    private String token;

    public SalasanaUusiView(SalasananPalautusService palautusService) {
        this.palautusService = palautusService;
        addClassName("auth-view");
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        kortti.addClassName("auth-kortti");
        kortti.setWidth("min(440px, 100%)");
        kortti.setPadding(false);
        add(kortti);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        token = event.getLocation().getQueryParameters().getSingleParameter("token").orElse(null);
        kortti.removeAll();
        kortti.add(new H2("Aseta uusi salasana"));
        if (!palautusService.onKelvollinen(token)) {
            kortti.add(new Paragraph("Linkki on vanhentunut tai sitä on jo käytetty."),
                    new RouterLink("Pyydä uusi linkki", SalasanaUnohtuiView.class));
            return;
        }
        PasswordField uusi = new PasswordField("Uusi salasana");
        uusi.setMinLength(KayttajaService.SALASANAN_MIN_PITUUS);
        uusi.setHelperText("Vähintään " + KayttajaService.SALASANAN_MIN_PITUUS + " merkkiä");
        uusi.setWidthFull();
        PasswordField toisto = new PasswordField("Salasana uudelleen");
        toisto.setWidthFull();
        Button tallenna = new Button("Vaihda salasana", e -> {
            if (!uusi.getValue().equals(toisto.getValue())) {
                toisto.setErrorMessage("Salasanat eivät täsmää");
                toisto.setInvalid(true);
                return;
            }
            try {
                palautusService.vaihdaSalasana(token, uusi.getValue());
                Ilmoitus.onnistui("Salasana vaihdettu. Voit nyt kirjautua uudella salasanalla.");
                getUI().ifPresent(ui -> ui.navigate(LoginView.class));
            } catch (LiiketoimintaVirhe ex) {
                Ilmoitus.virhe(ex.getMessage());
            }
        });
        tallenna.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        tallenna.setWidthFull();
        kortti.add(uusi, toisto, tallenna);
    }
}
