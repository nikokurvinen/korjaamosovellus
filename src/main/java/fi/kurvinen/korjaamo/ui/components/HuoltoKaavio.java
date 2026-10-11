package fi.kurvinen.korjaamo.ui.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.shared.Registration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ulkoinen JavaScript-komponentti Chart.js Vaadin-komponenttina (vaatimus 33).
 * <p>
 * {@code @NpmPackage} lisää chart.js-kirjaston package.jsoniin, ja Vaadin asentaa sen npm:llä
 * buildin yhteydessä. {@code @JsModule} lataa TypeScript-kääreen, joka rekisteröi
 * {@code <huolto-kaavio>}-elementin. Java-puolelta kaavio määritellään tavallisilla Map/List-
 * rakenteilla, jotka sarjallistetaan JSONiksi ja asetetaan elementin propertyksi.
 */
@Tag("huolto-kaavio")
@NpmPackage(value = "chart.js", version = "4.4.7")
@JsModule("./components/huolto-kaavio.ts")
public class HuoltoKaavio extends Component implements HasSize, HasStyle {

    private static final ObjectMapper JSON = new ObjectMapper();

    public HuoltoKaavio() {
        setWidthFull();
        setHeight("280px");
    }

    /** Donitsikaavio: yksi arvo per sektori. */
    public void naytaDonitsi(List<String> otsikot, List<? extends Number> arvot, List<String> varit) {
        Map<String, Object> dataset = new LinkedHashMap<>();
        dataset.put("data", arvot);
        dataset.put("backgroundColor", varit);
        dataset.put("borderWidth", 2);
        asetaConfig(Map.of(
                "type", "doughnut",
                "data", Map.of("labels", otsikot, "datasets", List.of(dataset)),
                "options", Map.of(
                        "cutout", "62%",
                        "plugins", Map.of("legend", Map.of("position", "bottom")))));
    }

    /** Yhdistelmäkaavio: pylväät (määrä) ja viiva (liikevaihto) omilla y-akseleillaan. */
    public void naytaPylvasJaViiva(List<String> otsikot, String pylvasNimi, List<? extends Number> pylvaat,
                                   String viivaNimi, List<? extends Number> viiva) {
        Map<String, Object> pylvasData = new LinkedHashMap<>();
        pylvasData.put("type", "bar");
        pylvasData.put("label", pylvasNimi);
        pylvasData.put("data", pylvaat);
        pylvasData.put("backgroundColor", "hsla(205, 35%, 45%, 0.75)");
        pylvasData.put("borderRadius", 3);
        pylvasData.put("yAxisID", "y");

        Map<String, Object> viivaData = new LinkedHashMap<>();
        viivaData.put("type", "line");
        viivaData.put("label", viivaNimi);
        viivaData.put("data", viiva);
        viivaData.put("borderColor", "hsl(24, 95%, 50%)");
        viivaData.put("backgroundColor", "hsl(24, 95%, 50%)");
        viivaData.put("tension", 0.35);
        viivaData.put("yAxisID", "y1");

        asetaConfig(Map.of(
                "type", "bar",
                "data", Map.of("labels", otsikot, "datasets", List.of(pylvasData, viivaData)),
                "options", Map.of(
                        "plugins", Map.of("legend", Map.of("position", "bottom")),
                        "scales", Map.of(
                                "y", Map.of("beginAtZero", true, "position", "left", "ticks", Map.of("precision", 0)),
                                "y1", Map.of("beginAtZero", true, "position", "right",
                                        "grid", Map.of("drawOnChartArea", false))))));
    }

    private void asetaConfig(Map<String, Object> config) {
        try {
            getElement().setProperty("config", JSON.writeValueAsString(config));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public Registration addSegmenttiKlikattuListener(ComponentEventListener<SegmenttiKlikattuEvent> listener) {
        return addListener(SegmenttiKlikattuEvent.class, listener);
    }

    /** Selaimen CustomEvent → palvelimen Java-event. {@code @EventData} poimii detail.labelin. */
    @DomEvent("segmentti-klikattu")
    public static class SegmenttiKlikattuEvent extends ComponentEvent<HuoltoKaavio> {
        private final String otsikko;

        public SegmenttiKlikattuEvent(HuoltoKaavio source, boolean fromClient,
                                      @EventData("event.detail.label") String otsikko) {
            super(source, fromClient);
            this.otsikko = otsikko;
        }

        public String getOtsikko() { return otsikko; }
    }
}
