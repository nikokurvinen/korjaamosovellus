package fi.kurvinen.korjaamo.config;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import fi.kurvinen.korjaamo.ui.MainLayout;
import org.springframework.stereotype.Component;

/**
 * Oletuskieli (vaatimus 23). Vaadin valitsisi kielen selaimen asetuksista, jolloin sama sovellus
 * näkyisi eri koneilla eri kielellä. Kohdeyleisö on suomalainen korjaamo, joten uusi istunto alkaa
 * aina suomeksi; käyttäjän headerista valitsema kieli tallentuu session localeksi.
 */
@Component
public class KieliAsetus implements VaadinServiceInitListener {

    static final String KIELI_ASETETTU = "korjaamo.kieli-asetettu";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(e -> {
            VaadinSession session = e.getUI().getSession();
            if (session.getAttribute(KIELI_ASETETTU) == null) {
                session.setAttribute(KIELI_ASETETTU, Boolean.TRUE);
                session.setLocale(MainLayout.SUOMI);
            }
            e.getUI().setLocale(session.getLocale());
        });
    }
}
