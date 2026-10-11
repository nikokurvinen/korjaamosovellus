package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import fi.kurvinen.korjaamo.service.SalasananPalautusService;
import fi.kurvinen.korjaamo.ui.MainLayout;

/**
 * Salasanan palautuspyyntö (vaatimus 27). Vastaus on aina sama riippumatta siitä, löytyikö
 * osoitetta, jotta lomakkeella ei voi selvittää rekisteröityjä sähköpostiosoitteita.
 */
@Route(value = "salasana/unohtui", layout = MainLayout.class)
@PageTitle("Salasana unohtui | Korjaamo")
@AnonymousAllowed
public class SalasanaUnohtuiView extends VerticalLayout {

    public SalasanaUnohtuiView(SalasananPalautusService palautusService) {
        addClassName("auth-view");
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        EmailField email = new EmailField("Sähköposti");
        email.setWidthFull();
        email.setRequiredIndicatorVisible(true);
        email.setErrorMessage("Anna kelvollinen sähköpostiosoite");
        Paragraph tulos = new Paragraph();
        tulos.setVisible(false);
        tulos.getStyle().set("color", "var(--lumo-success-text-color)");

        Button laheta = new Button("Lähetä vaihtolinkki");
        laheta.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        laheta.setWidthFull();
        laheta.addClickListener(e -> {
            if (email.isEmpty() || email.isInvalid()) {
                email.setInvalid(true);
                return;
            }
            palautusService.pyydaPalautus(email.getValue());
            tulos.setText("Jos osoitteella on tunnus, siihen lähetettiin linkki salasanan vaihtoon. "
                    + "Linkki on voimassa 30 minuuttia.");
            tulos.setVisible(true);
            laheta.setEnabled(false);
        });

        VerticalLayout kortti = new VerticalLayout(new H2("Salasana unohtui?"),
                new Paragraph("Anna tunnukseesi liitetty sähköpostiosoite, niin lähetämme linkin uuden salasanan asettamiseen."),
                email, laheta, tulos, new RouterLink("Takaisin kirjautumiseen", LoginView.class));
        kortti.addClassName("auth-kortti");
        kortti.setWidth("min(440px, 100%)");
        kortti.setPadding(false);
        add(kortti);
    }
}
