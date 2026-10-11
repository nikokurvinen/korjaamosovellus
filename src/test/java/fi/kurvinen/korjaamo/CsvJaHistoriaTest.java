package fi.kurvinen.korjaamo;

import fi.kurvinen.korjaamo.domain.Asiakas;
import fi.kurvinen.korjaamo.service.AsiakasService;
import fi.kurvinen.korjaamo.service.CsvService;
import fi.kurvinen.korjaamo.service.HistoriaService;
import fi.kurvinen.korjaamo.service.HistoriaService.HistoriaMerkinta;
import fi.kurvinen.korjaamo.service.HistoriaService.Toiminto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** CSV-tuonti ja -vienti (29), Spring Auditing (30) ja Envers-historia (31–32). */
@SpringBootTest
@ActiveProfiles("test")
class CsvJaHistoriaTest {

    @Autowired CsvService csvService;
    @Autowired AsiakasService asiakasService;
    @Autowired HistoriaService historiaService;

    @Test
    void csvTuontiValidoiRivitJaVientiSisaltaaBomin() {
        String csv = """
                tuotenumero;nimi;valmistaja;hinta;varastosaldo
                TST-001;Testiosa;Bosch;12,50;5
                TST-002;Toinen osa;NGK;7.90;0
                huono;;Bosch;-1;2
                TST-003;Kolmas;Mahle;ei-numero;1
                """;
        CsvService.TuontiTulos tulos = csvService.tuoVaraosat(csv.getBytes(StandardCharsets.UTF_8));
        assertThat(tulos.lisatty()).isEqualTo(2);
        assertThat(tulos.virheet()).hasSize(2);
        assertThat(tulos.virheet().get(0)).startsWith("Rivi 4");

        byte[] vienti = csvService.vieVaraosat();
        assertThat(vienti[0] & 0xFF).isEqualTo(0xEF); // UTF-8 BOM Exceliä varten
        assertThat(new String(vienti, StandardCharsets.UTF_8)).contains("TST-001;Testiosa;Bosch;12,50;5");
    }

    @Test
    @WithMockUser(username = "mekaanikko")
    void auditointiJaHistoriaTallentavatTekijan() {
        Asiakas a = asiakasService.tallenna(TestiData.asiakas("Historia", "Testi", "historia@example.fi"));
        assertThat(a.getLuonut()).isEqualTo("mekaanikko");
        assertThat(a.getLuotu()).isNotNull();

        a.setKaupunki("Oulu");
        asiakasService.tallenna(a);

        List<HistoriaMerkinta> historia = historiaService.entiteetinHistoria(Asiakas.class, a.getId());
        assertThat(historia).hasSize(2);
        assertThat(historia.get(0).toiminto()).isEqualTo(Toiminto.MUUTOS);
        assertThat(historia.get(0).muuttuneetKentat()).containsExactly("kaupunki");
        assertThat(historia.get(0).kayttaja()).isEqualTo("mekaanikko");
        assertThat(historia.get(1).toiminto()).isEqualTo(Toiminto.LISAYS);
    }
}
