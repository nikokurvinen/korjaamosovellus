package fi.kurvinen.korjaamo.ui.components;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/** Yhtenäiset onnistumis-, virhe- ja infoilmoitukset kaikkiin näkymiin. */
public final class Ilmoitus {

    private Ilmoitus() {
    }

    public static void onnistui(String teksti) {
        nayta(teksti, NotificationVariant.LUMO_SUCCESS, 3000);
    }

    public static void virhe(String teksti) {
        nayta(teksti, NotificationVariant.LUMO_ERROR, 6000);
    }

    public static void info(String teksti) {
        nayta(teksti, NotificationVariant.LUMO_CONTRAST, 4000);
    }

    private static void nayta(String teksti, NotificationVariant variantti, int kesto) {
        Notification n = Notification.show(teksti, kesto, Notification.Position.BOTTOM_END);
        n.addThemeVariants(variantti);
    }
}
