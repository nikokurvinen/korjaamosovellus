package fi.kurvinen.korjaamo.ui.components;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;

import java.time.Year;

/**
 * Sovelluksen footer (vaatimus 15): tekijä, copyright, linkit ja versio.
 * Kiinnitys alareunaan ja responsiivisuus tulevat teeman .app-footer-säännöistä.
 */
public class AppFooter extends Footer implements LocaleChangeObserver {

    public static final String GITHUB_URL = "https://github.com/nikokurvinen/korjaamosovellus";

    private final Span tekija = new Span();
    private final Anchor github = new Anchor(GITHUB_URL, "GitHub", AnchorTarget.BLANK);
    private final Anchor vaadin = new Anchor("https://vaadin.com/docs/latest", "Vaadin", AnchorTarget.BLANK);
    private final Span versio = new Span();

    public AppFooter() {
        addClassName("app-footer");
        Div linkit = new Div(github, vaadin, versio);
        linkit.addClassName("footer-links");
        add(tekija, linkit);
    }

    @Override
    public void localeChange(LocaleChangeEvent event) {
        tekija.setText("© " + Year.now() + " Niko Kurvinen · " + getTranslation("footer.kuvaus"));
        versio.setText(getTranslation("footer.versio", "1.0.0"));
    }
}
