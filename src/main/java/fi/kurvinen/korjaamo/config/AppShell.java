package fi.kurvinen.korjaamo.config;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;

/**
 * Sovelluksen "kuori", jonka asetukset koskevat koko SPA:ta.
 * <ul>
 *   <li>{@code @Theme("korjaamoteema")} lataa globaalit tyylit (vaatimus 6) ja Lumo Utility
 *       -luokat (theme.json → lumoImports, vaatimus 9).</li>
 *   <li>{@code @Push} avaa WebSocket-yhteyden, jonka kautta palvelin voi päivittää näkymiä ilman,
 *       että käyttäjä tekee mitään (vaatimus 22).</li>
 * </ul>
 */
@Theme("korjaamoteema")
@Push
@Meta(name = "author", content = "Niko Kurvinen")
public class AppShell implements AppShellConfigurator {
}
