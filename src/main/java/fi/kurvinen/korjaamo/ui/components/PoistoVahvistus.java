package fi.kurvinen.korjaamo.ui.components;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

/** Poisto vaatii aina vahvistuksen – vahingossa painettua poistoa ei voi perua. */
public final class PoistoVahvistus {

    private PoistoVahvistus() {
    }

    public static void kysy(String kohde, Runnable poista) {
        ConfirmDialog d = new ConfirmDialog();
        d.setHeader("Poistetaanko " + kohde + "?");
        d.setText("Poistoa ei voi perua. Poistettu tieto säilyy kuitenkin muutoshistoriassa.");
        d.setCancelable(true);
        d.setCancelText("Peruuta");
        d.setConfirmText("Poista");
        d.setConfirmButtonTheme("error primary");
        d.addConfirmListener(e -> poista.run());
        d.open();
    }
}
