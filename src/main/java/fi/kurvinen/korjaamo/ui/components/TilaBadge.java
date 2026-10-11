package fi.kurvinen.korjaamo.ui.components;

import com.vaadin.flow.component.html.Span;
import fi.kurvinen.korjaamo.domain.HuoltoTila;

/**
 * Huoltotilan värillinen merkki. Esimerkki vaatimuksen 7 kahdesta tyylitavasta samassa
 * komponentissa:
 * <ul>
 *   <li>{@code addClassName("tila-badge")} – yhteinen ulkoasu tulee teeman CSS-luokasta</li>
 *   <li>{@code getStyle().set("background", ...)} – tilakohtainen väri asetetaan ajonaikaisesti,
 *       koska se riippuu datasta eikä sitä kannata kirjoittaa neljänä erillisenä CSS-luokkana</li>
 * </ul>
 */
public class TilaBadge extends Span {

    public TilaBadge(HuoltoTila tila) {
        super(tila.getNimi());
        addClassName("tila-badge");
        getStyle().set("background", tila.getVari());
    }
}
