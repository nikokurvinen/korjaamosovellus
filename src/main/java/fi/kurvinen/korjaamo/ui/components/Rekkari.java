package fi.kurvinen.korjaamo.ui.components;

import com.vaadin.flow.component.html.Span;

/** Rekisterikilven näköinen merkintä (CSS-luokka .rekkari globaalissa teemassa). */
public class Rekkari extends Span {

    public Rekkari(String rekisterinumero) {
        super(rekisterinumero);
        addClassName("rekkari");
    }
}
