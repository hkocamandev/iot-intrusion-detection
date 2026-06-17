package com.github.hkocamandev.iot_intrusion_detection.producer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.simulator.enabled", havingValue = "true")
public class SimulatorRunner implements ApplicationRunner {

    private final CsvReplaySimulator simulator;
    private final String csvPath;
    private final long delayMs;

    public SimulatorRunner(CsvReplaySimulator simulator,
                           @Value("${app.simulator.csv-path}") String csvPath,
                           @Value("${app.simulator.delay-ms}") long delayMs) {
        this.simulator = simulator;
        this.csvPath = csvPath;
        this.delayMs = delayMs;
    }

    @Override
    public void run(ApplicationArguments args) {
        new Thread(() -> simulator.replay(csvPath, delayMs), "csv-simulator").start();
    }
}
