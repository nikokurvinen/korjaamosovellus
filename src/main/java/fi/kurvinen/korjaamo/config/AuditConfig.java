package fi.kurvinen.korjaamo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Ottaa Spring Data JPA Auditingin käyttöön (vaatimus 30). Tekijän tiedot tulevat
 * {@link fi.kurvinen.korjaamo.security.SecurityAuditorAware}-beanista.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditConfig {
}
