# syntax=docker/dockerfile:1
#
# Korjaamosovelluksen Docker-image (vaatimus 24).
#
# Vaihe 1 (build): JDK + Maven Wrapper kääntävät sovelluksen ja Vaadinin tuotantobundlen.
#                  Vaadin-plugin lataa Node.js:n itse, joten sitä ei tarvitse asentaa imageen.
# Vaihe 2 (ajo):   pelkkä JRE ja valmis jar. Lopullisessa imagessa ei ole kääntäjää, Mavenia,
#                  Nodea eikä lähdekoodia – image on pienempi ja hyökkäyspinta kapeampi.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Riippuvuudet omaan kerrokseensa: niin kauan kuin pom.xml ei muutu, Docker käyttää välimuistia
# eikä koodimuutos lataa satoja megatavuja riippuvuuksia uudelleen.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q -Pproduction dependency:go-offline

COPY src src
RUN ./mvnw -B -Pproduction -DskipTests package \
    && cp target/korjaamosovellus-*.jar /src/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

# Ei-root-käyttäjä: mahdollinen haavoittuvuus sovelluksessa ei anna pääkäyttäjän oikeuksia konttiin.
RUN groupadd --system korjaamo && useradd --system --gid korjaamo korjaamo \
    && mkdir -p /data/liitteet && chown -R korjaamo:korjaamo /data
USER korjaamo

COPY --from=build /src/app.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker \
    APP_UPLOAD_DIR=/data/liitteet \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
VOLUME /data/liitteet

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
