package fi.kurvinen.korjaamo.service;

import fi.kurvinen.korjaamo.domain.Varaosa;
import fi.kurvinen.korjaamo.repository.VaraosaRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Varaosaluettelon CSV-vienti ja -tuonti (vaatimus 29).
 * <p>
 * Muotovalinnat on tehty suomalaista Exceliä varten: erottimena puolipiste (pilkku on desimaalierotin)
 * ja tiedoston alussa UTF-8 BOM, jota ilman Excel avaa tiedoston väärällä merkistöllä ja ääkköset
 * rikkoutuvat. Tuonti hyväksyy sekä pilkku- että pistedesimaalit.
 */
@Service
public class CsvService {

    static final String[] OTSIKOT = {"tuotenumero", "nimi", "valmistaja", "hinta", "varastosaldo"};
    private static final CSVFormat MUOTO = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader(OTSIKOT)
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .get();

    private final VaraosaRepository repository;
    private final Validator validator;
    private final MuutosJulkaisija julkaisija;

    public CsvService(VaraosaRepository repository, Validator validator, MuutosJulkaisija julkaisija) {
        this.repository = repository;
        this.validator = validator;
        this.julkaisija = julkaisija;
    }

    @Transactional(readOnly = true)
    public byte[] vieVaraosat() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            w.write('﻿');
            try (CSVPrinter printer = new CSVPrinter(w, CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader(OTSIKOT).get())) {
                for (Varaosa v : repository.findAllByOrderByNimiAsc()) {
                    printer.printRecord(v.getTuotenumero(), v.getNimi(), v.getValmistaja(),
                            v.getHinta().toPlainString().replace('.', ','), v.getVarastosaldo());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /**
     * Tuo varaosat. Olemassa oleva tuotenumero päivitetään, uusi lisätään. Jokainen rivi validoidaan
     * samoilla Bean Validation -säännöillä kuin lomake; virheelliset rivit raportoidaan rivinumeron
     * kanssa eikä niitä tallenneta. Kelvolliset rivit tallennetaan samassa transaktiossa.
     */
    @Transactional
    public TuontiTulos tuoVaraosat(byte[] data) {
        List<String> virheet = new ArrayList<>();
        int lisatty = 0;
        int paivitetty = 0;
        try (Reader reader = new InputStreamReader(ohitaBom(data), StandardCharsets.UTF_8);
             CSVParser parser = MUOTO.parse(reader)) {
            for (CSVRecord rivi : parser) {
                long nro = rivi.getRecordNumber() + 1; // +1 = otsikkorivi, jotta numero vastaa Excelin riviä
                try {
                    String tuotenumero = rivi.get("tuotenumero").toUpperCase();
                    Varaosa v = repository.findByTuotenumero(tuotenumero).orElseGet(Varaosa::new);
                    boolean uusi = v.isNew();
                    v.setTuotenumero(tuotenumero);
                    v.setNimi(rivi.get("nimi"));
                    v.setValmistaja(rivi.get("valmistaja"));
                    v.setHinta(new BigDecimal(rivi.get("hinta").replace(',', '.').replace(" ", "")));
                    v.setVarastosaldo(Integer.valueOf(rivi.get("varastosaldo")));

                    Set<ConstraintViolation<Varaosa>> rikkeet = validator.validate(v);
                    if (!rikkeet.isEmpty()) {
                        virheet.add("Rivi " + nro + ": " + rikkeet.stream()
                                .map(r -> r.getPropertyPath() + " – " + r.getMessage())
                                .collect(Collectors.joining("; ")));
                        continue;
                    }
                    repository.save(v);
                    if (uusi) lisatty++; else paivitetty++;
                } catch (NumberFormatException e) {
                    virheet.add("Rivi " + nro + ": hinta tai saldo ei ole numero");
                } catch (IllegalArgumentException | IllegalStateException e) {
                    virheet.add("Rivi " + nro + ": sarakkeita puuttuu");
                }
            }
        } catch (IOException | UncheckedIOException e) {
            throw new LiiketoimintaVirhe("CSV-tiedostoa ei voitu lukea: " + e.getMessage());
        }
        if (lisatty + paivitetty > 0) {
            julkaisija.julkaise("Varaosa", "CSV-tuonti", lisatty + " lisätty, " + paivitetty + " päivitetty");
        }
        return new TuontiTulos(lisatty, paivitetty, virheet);
    }

    private static PushbackInputStream ohitaBom(byte[] data) throws IOException {
        PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(data), 3);
        byte[] alku = new byte[3];
        int n = in.read(alku, 0, 3);
        boolean bom = n == 3 && (alku[0] & 0xFF) == 0xEF && (alku[1] & 0xFF) == 0xBB && (alku[2] & 0xFF) == 0xBF;
        if (!bom && n > 0) {
            in.unread(alku, 0, n);
        }
        return in;
    }

    public record TuontiTulos(int lisatty, int paivitetty, List<String> virheet) {
    }
}
