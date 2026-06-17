package com.github.hkocamandev.iot_intrusion_detection.producer;

import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class CsvReplaySimulator {

    private static final Logger log = LoggerFactory.getLogger(CsvReplaySimulator.class);

    private static final Set<String> NON_FEATURE = Set.of(
            "proto", "service", "attack_type", "label", "id.orig_p", "id.resp_p");

    private final TrafficProducer producer;

    public CsvReplaySimulator(TrafficProducer producer) {
        this.producer = producer;
    }

    public void replay(String csvPath, long delayMs) {
        long count = 0;
        try (Reader reader = Files.newBufferedReader(Path.of(csvPath));
             CSVParser parser = CSVParser.parse(reader,
                     CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

            for (CSVRecord record : parser) {
                producer.publish(toEvent(record));
                count++;
                if (delayMs > 0) Thread.sleep(delayMs);
            }
            log.info("Replayed {} flows from {}", count, csvPath);
        } catch (IOException e) {
            throw new IllegalStateException("CSV okunamadı: " + csvPath, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Replay interrupted after {} flows from {}", count, csvPath);
        }
    }

    private TrafficEvent toEvent(CSVRecord record) {
        Map<String, Double> features = new LinkedHashMap<>();
        String proto = null, service = null, label = null;

        for (Map.Entry<String, String> cell : record.toMap().entrySet()) {
            String key = cell.getKey();
            String value = cell.getValue();
            String keyLower = key.toLowerCase();

            if (keyLower.equals("proto")) { proto = value; continue; }
            if (keyLower.equals("service")) { service = "-".equals(value) ? null : value; continue; }
            if (keyLower.equals("attack_type") || keyLower.equals("label")) { label = value; continue; }
            if (NON_FEATURE.contains(keyLower)) continue;

            Double num = parseDouble(value);
            if (num != null) features.put(key, num);
        }

        String sourceId = (proto == null ? "unknown" : proto) + "-" + Math.floorMod(record.hashCode(), 50);
        return new TrafficEvent(Instant.now(), sourceId, proto, service, label, features);
    }

    private Double parseDouble(String s) {
        if (s == null || s.isBlank() || s.equals("-")) return null;
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}
