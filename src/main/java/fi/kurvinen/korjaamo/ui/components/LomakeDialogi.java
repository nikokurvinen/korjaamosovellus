package fi.kurvinen.korjaamo.ui.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValidationResult;
import fi.kurvinen.korjaamo.domain.AbstractAuditableEntity;
import fi.kurvinen.korjaamo.service.LiiketoimintaVirhe;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Yleiskäyttöinen lisäys- ja muokkauslomake dialogissa (CRUD: tallennus- ja muokkauslomake, vaatimus 1–4).
 * <p>
 * {@link BeanValidationBinder} lukee entiteetin JSR-380-annotaatiot (vaatimus 5) ja näyttää
 * virheet suoraan kenttien alla jo kirjoitettaessa. Sama validointi ajetaan vielä JPA:n
 * tallennuksessa, joten käyttöliittymän ohittaminen ei päästä virheellistä dataa kantaan.
 * <p>
 * Lomake muokkaa entiteetin <i>kopiota</i> ({@code writeBean}) vasta tallennettaessa: jos käyttäjä
 * peruuttaa, gridissä näkyvä olio pysyy koskemattomana.
 *
 * @param <T> muokattava entiteetti
 */
public class LomakeDialogi<T extends AbstractAuditableEntity> extends Dialog {

    private static final DateTimeFormatter AIKA = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm");

    private final BeanValidationBinder<T> binder;
    private final FormLayout lomake = new FormLayout();
    private final Span yleisvirhe = new Span();
    private final Span auditointi = new Span();
    private final Button tallenna = new Button("Tallenna", VaadinIcon.CHECK.create());
    private final Button poista = new Button("Poista", VaadinIcon.TRASH.create());
    private final String otsikkoUusi;
    private final String otsikkoMuokkaa;
    private T bean;
    private Consumer<T> tallennusToiminto;
    private Consumer<T> poistoToiminto;

    public LomakeDialogi(Class<T> tyyppi, String otsikkoUusi, String otsikkoMuokkaa) {
        this.binder = new BeanValidationBinder<>(tyyppi);
        this.otsikkoUusi = otsikkoUusi;
        this.otsikkoMuokkaa = otsikkoMuokkaa;
        setWidth("min(720px, 95vw)");
        setDraggable(true);
        setCloseOnOutsideClick(false);

        lomake.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2));

        yleisvirhe.getStyle().set("color", "var(--lumo-error-text-color)");
        yleisvirhe.setVisible(false);
        auditointi.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");
        add(lomake, yleisvirhe, auditointi);

        tallenna.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        tallenna.addClickListener(e -> tallenna());
        poista.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        poista.addClickListener(e -> PoistoVahvistus.kysy(bean.toString(), () -> {
            suorita(() -> poistoToiminto.accept(bean));
        }));
        Button peruuta = new Button("Peruuta", e -> close());

        HorizontalLayout napit = new HorizontalLayout(poista, peruuta, tallenna);
        napit.setWidthFull();
        napit.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        poista.getStyle().set("margin-inline-end", "auto");
        getFooter().add(napit);

    }

    public BeanValidationBinder<T> getBinder() {
        return binder;
    }

    public void lisaa(Component... kentat) {
        lomake.add(kentat);
    }

    public void lisaaLeveana(Component kentta) {
        lomake.add(kentta);
        lomake.setColspan(kentta, 2);
    }

    public void kunTallennetaan(Consumer<T> toiminto) {
        this.tallennusToiminto = toiminto;
    }

    public void kunPoistetaan(Consumer<T> toiminto) {
        this.poistoToiminto = toiminto;
    }

    /** Avaa lomakkeen. {@code tehdas} luo uuden olion, jos {@code muokattava} on null. */
    public void avaa(T muokattava, Supplier<T> tehdas) {
        this.bean = muokattava != null ? muokattava : tehdas.get();
        setHeaderTitle(bean.isNew() ? otsikkoUusi : otsikkoMuokkaa);
        binder.readBean(bean);
        poista.setVisible(!bean.isNew() && poistoToiminto != null);
        yleisvirhe.setVisible(false);
        if (bean.isNew()) {
            auditointi.setText("");
        } else {
            auditointi.setText("Luotu " + muotoile(bean.getLuotu()) + " (" + bean.getLuonut() + ") · Muokattu "
                    + muotoile(bean.getMuokattu()) + " (" + bean.getMuokannut() + ")");
        }
        open();
    }

    private void tallenna() {
        try {
            binder.writeBean(bean);
        } catch (ValidationException e) {
            // Luokkatason (@AssertTrue) virheet eivät kuulu mihinkään kenttään, joten ne näytetään erikseen.
            paivitaYleisvirhe(binder.validate());
            return;
        }
        suorita(() -> tallennusToiminto.accept(bean));
    }

    private void suorita(Runnable toiminto) {
        try {
            toiminto.run();
            close();
        } catch (LiiketoimintaVirhe e) {
            yleisvirhe.setText(e.getMessage());
            yleisvirhe.setVisible(true);
            Ilmoitus.virhe(e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            Ilmoitus.virhe("Joku muu muokkasi tietoa samaan aikaan. Avaa lomake uudelleen.");
            close();
        }
    }

    private void paivitaYleisvirhe(BinderValidationStatus<T> status) {
        String viestit = status.getBeanValidationErrors().stream()
                .map(ValidationResult::getErrorMessage)
                .collect(Collectors.joining(" "));
        yleisvirhe.setText(viestit);
        yleisvirhe.setVisible(!viestit.isEmpty());
    }

    private static String muotoile(java.time.LocalDateTime t) {
        return t == null ? "–" : AIKA.format(t);
    }
}
