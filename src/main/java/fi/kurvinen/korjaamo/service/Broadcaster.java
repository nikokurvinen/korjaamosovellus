package fi.kurvinen.korjaamo.service;

import com.vaadin.flow.shared.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Server Push -viestinvälittäjä (vaatimus 22).
 * <p>
 * Palvelut julkaisevat {@link MuutosTapahtuma}n Springin event-väylään. Tämä luokka vastaanottaa
 * sen vasta <b>transaktion commitin jälkeen</b> ({@link TransactionalEventListener}), jotta muut
 * käyttäjät eivät koskaan näe muutosta, joka myöhemmin peruuntuu rollbackissa.
 * <p>
 * Näkymät rekisteröivät kuuntelijan {@code onAttach}-vaiheessa ja poistavat sen {@code onDetach}issa.
 * Kuuntelijat kutsutaan omassa säikeessään, koska kukin kuuntelija lukitsee oman UI:nsa
 * ({@code ui.access}); jos tallentajan säie tekisi sen suoraan, se voisi jäädä odottamaan
 * toisen käyttäjän istuntolukkoa.
 */
@Component
public class Broadcaster {

    private static final Logger log = LoggerFactory.getLogger(Broadcaster.class);
    private static final int HISTORIAN_KOKO = 8;

    private final List<Consumer<MuutosTapahtuma>> kuuntelijat = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "korjaamo-broadcaster");
        t.setDaemon(true);
        return t;
    });
    private final Deque<MuutosTapahtuma> viimeisimmat = new ArrayDeque<>();

    public Registration rekisteroi(Consumer<MuutosTapahtuma> kuuntelija) {
        kuuntelijat.add(kuuntelija);
        return () -> kuuntelijat.remove(kuuntelija);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void kunMuutosTallennettu(MuutosTapahtuma tapahtuma) {
        synchronized (viimeisimmat) {
            viimeisimmat.addFirst(tapahtuma);
            while (viimeisimmat.size() > HISTORIAN_KOKO) {
                viimeisimmat.removeLast();
            }
        }
        for (Consumer<MuutosTapahtuma> k : kuuntelijat) {
            executor.execute(() -> {
                try {
                    k.accept(tapahtuma);
                } catch (RuntimeException e) {
                    // Suljettu selainvälilehti ei saa estää viestin välittämistä muille.
                    log.debug("Push-kuuntelija epäonnistui", e);
                }
            });
        }
    }

    /** Dashboardin "Live-muutokset" -lista näyttää heti avattaessa myös aiemmat tapahtumat. */
    public List<MuutosTapahtuma> viimeisimmat() {
        synchronized (viimeisimmat) {
            return List.copyOf(viimeisimmat);
        }
    }
}
