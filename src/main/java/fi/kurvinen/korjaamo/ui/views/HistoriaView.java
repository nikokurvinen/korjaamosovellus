package fi.kurvinen.korjaamo.ui.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.server.auth.AccessDeniedErrorRouter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import fi.kurvinen.korjaamo.service.HistoriaService;
import fi.kurvinen.korjaamo.ui.MainLayout;
import fi.kurvinen.korjaamo.ui.components.HistoriaLista;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.List;

/**
 * Koko sovelluksen muutoshistoria aikajanana – vain ADMIN (vaatimukset 31–32).
 * <p>
 * Layout-tyyppi (vaatimus 12d): <b>aikajana</b> ja välilehdet entiteettityypeittäin. Jokainen
 * kohta näyttää ajan, tekijän (oma Envers-revisioentiteetti), muutostyypin ja muuttuneet kentät.
 */
@Route(value = "admin/historia", layout = MainLayout.class)
@PageTitle("Muutoshistoria | Korjaamo")
@RolesAllowed("ADMIN")
@AccessDeniedErrorRouter
public class HistoriaView extends VerticalLayout {

    private static final int MAKSIMI = 100;

    public HistoriaView(HistoriaService service) {
        setMaxWidth("900px");
        getStyle().set("margin", "0 auto");

        List<Class<?>> tyypit = new ArrayList<>();
        tyypit.add(null);
        Tabs tabs = new Tabs(new Tab("Kaikki"));
        HistoriaService.KOHTEET.forEach((luokka, nimi) -> {
            tabs.add(new Tab(nimi));
            tyypit.add(luokka);
        });
        tabs.setWidthFull();

        HistoriaLista lista = new HistoriaLista();
        lista.setWidthFull();
        tabs.addSelectedChangeListener(e ->
                lista.nayta(service.viimeisimmat(tyypit.get(tabs.getSelectedIndex()), MAKSIMI), true));

        add(new H2("Muutoshistoria"),
                new Paragraph("Jokainen asiakkaisiin, ajoneuvoihin, vakuutuksiin, varaosiin ja huoltotilauksiin tehty "
                        + "muutos tallentuu Hibernate Enversin revisioksi. Näytetään " + MAKSIMI + " viimeisintä."),
                tabs, lista);
        lista.nayta(service.viimeisimmat(null, MAKSIMI), true);
    }
}
