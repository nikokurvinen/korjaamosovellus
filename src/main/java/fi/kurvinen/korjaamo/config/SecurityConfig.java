package fi.kurvinen.korjaamo.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import fi.kurvinen.korjaamo.ui.views.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security + Vaadin -integraatio (vaatimus 16).
 * <p>
 * Käytössä on Vaadin 24.9:ssä esitelty {@link VaadinSecurityConfigurer}, joka korvaa vanhan
 * {@code VaadinWebSecurity}-yläluokan. Se sallii Vaadinin sisäiset pyynnöt (bootstrap, Push,
 * staattiset resurssit) ja jättää näkymätason pääsynhallinnan {@code @AnonymousAllowed},
 * {@code @PermitAll} ja {@code @RolesAllowed} -annotaatioille, jotka tarkistetaan sekä
 * navigoinnissa että palvelimella.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/images/**", "/icons/**").permitAll());
        http.with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin.loginView(LoginView.class));
        return http.build();
    }

    /**
     * BCrypt: hidas ja suolattu tiiviste. Oletusvahvuus 10 tarkoittaa 2^10 kierrosta, mikä on
     * kirjautumisessa huomaamaton (~80 ms) mutta tekee massamurtamisesta erittäin kallista.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
