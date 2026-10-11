package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import fi.kurvinen.korjaamo.domain.Kayttaja;
import fi.kurvinen.korjaamo.security.KirjautunutKayttaja;
import fi.kurvinen.korjaamo.service.KayttajaService;
import fi.kurvinen.korjaamo.service.LiiketoimintaVirhe;
import fi.kurvinen.korjaamo.ui.MainLayout;
import fi.kurvinen.korjaamo.ui.components.Ilmoitus;
import jakarta.annotation.security.PermitAll;

import java.io.ByteArrayInputStream;

/**
 * Oma profiili ja profiilikuvan lisäys (vaatimus 19). Layout: keskitetty profiilikortti.
 * Kuva näkyy heti myös headerin Avatarissa, koska sivu ladataan tallennuksen jälkeen uudelleen.
 */
@Route(value = "profiili", layout = MainLayout.class)
@PageTitle("Oma profiili | Korjaamo")
@PermitAll
public class ProfiiliView extends VerticalLayout {

    private final KayttajaService service;
    private final KirjautunutKayttaja kirjautunut;
    private final Avatar avatar = new Avatar();

    public ProfiiliView(KayttajaService service, KirjautunutKayttaja kirjautunut) {
        this.service = service;
        this.kirjautunut = kirjautunut;
        setAlignItems(FlexComponent.Alignment.CENTER);

        Kayttaja k = kirjautunut.get().orElseThrow();
        String tunnus = k.getKayttajatunnus();

        avatar.setName(k.getNimi());
        avatar.getStyle().set("--vaadin-avatar-size", "128px").set("border", "4px solid var(--korjaamo-oranssi)");
        naytaKuva(k);

        UI ui = UI.getCurrent();
        Upload upload = new Upload(UploadHandler.inMemory((meta, data) -> {
            try {
                service.asetaProfiilikuva(tunnus, data, meta.contentType());
                // Koko sivu ladataan uudelleen, jotta myös headerin Avatar päivittyy.
                ui.access(() -> ui.getPage().reload());
            } catch (LiiketoimintaVirhe e) {
                ui.access(() -> Ilmoitus.virhe(e.getMessage()));
            }
        }));
        upload.setAcceptedFileTypes("image/png", "image/jpeg", ".png", ".jpg", ".jpeg");
        upload.setMaxFileSize((int) KayttajaService.PROFIILIKUVAN_MAX_KOKO);
        upload.setMaxFiles(1);
        upload.setUploadButton(new Button("Valitse kuva", VaadinIcon.CAMERA.create()));
        upload.setDropLabel(new Span("PNG tai JPEG, max 2 Mt"));
        upload.addFileRejectedListener(e -> Ilmoitus.virhe(e.getErrorMessage()));

        Button poistaKuva = new Button("Poista kuva", VaadinIcon.TRASH.create(), e -> {
            service.poistaProfiilikuva(tunnus);
            ui.getPage().reload();
        });
        poistaKuva.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        poistaKuva.setVisible(k.getProfiilikuva() != null);

        TextField nimi = new TextField("Nimi", k.getNimi(), "");
        nimi.setWidthFull();
        EmailField email = new EmailField("Sähköposti");
        email.setValue(k.getEmail());
        email.setWidthFull();
        Button tallenna = new Button("Tallenna tiedot", VaadinIcon.CHECK.create(), e -> {
            try {
                service.paivitaTiedot(tunnus, nimi.getValue(), email.getValue());
                Ilmoitus.onnistui("Tiedot päivitetty");
            } catch (LiiketoimintaVirhe ex) {
                Ilmoitus.virhe(ex.getMessage());
            }
        });
        tallenna.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Span tunnusTeksti = new Span("@" + tunnus + " · " + String.join(", ", k.getRoolit().stream().map(Enum::name).sorted().toList()));
        tunnusTeksti.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout kortti = new VerticalLayout(avatar, new H2(k.getNimi()), tunnusTeksti,
                new HorizontalLayout(upload, poistaKuva), nimi, email, tallenna);
        kortti.setAlignItems(FlexComponent.Alignment.CENTER);
        kortti.setWidth("min(480px, 100%)");
        kortti.getStyle().set("background", "var(--lumo-base-color)").set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)").set("border-top", "5px solid var(--korjaamo-oranssi)")
                .set("margin-top", "var(--lumo-space-l)");
        add(new Div(), kortti);
    }

    private void naytaKuva(Kayttaja k) {
        if (k.getProfiilikuva() != null) {
            byte[] kuva = k.getProfiilikuva();
            avatar.setImageHandler(DownloadHandler.fromInputStream(ev ->
                    new DownloadResponse(new ByteArrayInputStream(kuva), "profiili", k.getProfiilikuvanTyyppi(), kuva.length)));
        }
    }
}
