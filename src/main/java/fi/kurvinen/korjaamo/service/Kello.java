package fi.kurvinen.korjaamo.service;

import com.vaadin.flow.shared.Registration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Palvelimen kello, joka "tikittää" kerran sekunnissa kaikille rekisteröityneille näkymille.
 * Dashboardin kello on puhdas Server Push -esimerkki (vaatimus 22): selain ei pyydä mitään, vaan
 * palvelin lähettää päivityksen WebSocketin kautta. Yksi yhteinen ajastin on kevyempi kuin
 * oma säie jokaiselle avoimelle selainvälilehdelle.
 */
@Component
public class Kello {

    private final List<Consumer<LocalTime>> kuuntelijat = new CopyOnWriteArrayList<>();

    public Registration rekisteroi(Consumer<LocalTime> kuuntelija) {
        kuuntelijat.add(kuuntelija);
        return () -> kuuntelijat.remove(kuuntelija);
    }

    @Scheduled(fixedRate = 1000)
    void tikita() {
        LocalTime nyt = LocalTime.now().withNano(0);
        kuuntelijat.forEach(k -> k.accept(nyt));
    }
}
