# Korjaamo·Pro – autokorjaamon huoltotilausten hallinta

Vaadin 24 + Spring Boot 3.5 -harjoitustyö (Java web -kurssi). Tekijä: **Niko Kurvinen**.

Sovelluksella hallitaan autokorjaamon asiakkaita, ajoneuvoja, vakuutuksia, varaosia ja
huoltotilauksia. Työjono näytetään kanban-tauluna, joka päivittyy reaaliajassa kaikille käyttäjille
(Server Push). Dashboardin kaaviot on toteutettu ulkoisella Chart.js-kirjastolla.

## Käynnistys

### Docker (suositus)

```bash
docker compose up --build
```

| Palvelu   | Osoite                  |
|-----------|-------------------------|
| Sovellus  | http://localhost:8080   |
| Mailpit   | http://localhost:8025 (lähetetyt sähköpostit) |
| PostgreSQL| kontin sisällä (`db:5432`) |

### Kehitystila (H2-tiedostokanta)

Vaatii JDK 21+:n. Node.js ladataan automaattisesti.

```bash
./mvnw spring-boot:run
# sähköpostit näkyviin (valinnainen):
docker run -d -p 1025:1025 -p 8025:8025 axllent/mailpit
```

> **Huom:** Vaadinin kehityspalvelin ei toimi polussa, jossa on välilyöntejä tai ääkkösiä
> (esim. OneDrive-kansio). Kloonaa projekti tällöin esim. kansioon `~/dev/korjaamosovellus`.

### Testit

```bash
./mvnw test
```

## Demotunnukset

| Käyttäjä | Salasana  | Rooli | Näkee lisäksi |
|----------|-----------|-------|---------------|
| `admin`  | `admin123`| ADMIN | Käyttäjähallinta, Muutoshistoria |
| `super`  | `super123`| SUPER | Asiakkaat, Ajoneuvot, Vakuutukset, Varaosat |
| `user`   | `user123` | USER  | Asiakkaat, Ajoneuvot, Vakuutukset, Varaosat |

Kaikki näkevät etusivun. Kirjautuneet näkevät lisäksi työjonon, huoltotilaukset ja haun.

## Tekninen yhteenveto

| Osa-alue | Toteutus |
|----------|----------|
| Entiteetit | `Ajoneuvo` · `Vakuutus` (1:1) · `Asiakas` (1:N) · `Huoltotilaus` ↔ `Varaosa` (M:N) · `Liite` |
| Haku | Criteria API, `HuoltotilausRepositoryImpl` – dynaamiset predikaatit, LIKE, BETWEEN, JOIN, (OR) AND |
| Tyylit | Oma Lumo-teema `korjaamoteema`, näkymäkohtainen `tyojono-view.css`, Lumo Utility -luokat |
| Tietoturva | Spring Security + `VaadinSecurityConfigurer`, BCrypt, roolit ADMIN/SUPER/USER |
| Push | `@Push`, `Broadcaster` (commitin jälkeen), live-kello |
| i18n | fi/en – `vaadin-i18n/translations_*.properties` |
| Sähköposti | ilmoitus ylläpidolle rekisteröitymisestä, salasanan palautuslinkki (Mailpit) |
| Tiedostot | huoltotilausten liitteet levylle, profiilikuva, varaosien CSV-tuonti/-vienti |
| Historia | Spring Data Auditing + Hibernate Envers, aikajananäkymä |
| Ulkoinen JS | Chart.js 4 Lit-web-komponenttina (`huolto-kaavio.ts` + `HuoltoKaavio.java`) |

Tarkempi kuvaus jokaisesta vaatimuksesta on työraportissa.
