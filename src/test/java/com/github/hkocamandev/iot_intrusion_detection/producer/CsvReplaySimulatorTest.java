package com.github.hkocamandev.iot_intrusion_detection.producer;

import com.github.hkocamandev.iot_intrusion_detection.dto.TrafficEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvReplaySimulatorTest {

    @Test
    void parsesCsvIntoTrafficEvents() throws Exception {
        List<TrafficEvent> captured = new ArrayList<>();
        TrafficProducer fakeProducer = new TrafficProducer(null, "t") {
            @Override public void publish(TrafficEvent event) { captured.add(event); }
        };

        CsvReplaySimulator simulator = new CsvReplaySimulator(fakeProducer);
        String path = java.nio.file.Path.of(
                getClass().getResource("/sample-flows.csv").toURI()).toString();
        simulator.replay(path, 0);

        assertThat(captured).hasSize(3);
        TrafficEvent first = captured.get(0);
        assertThat(first.protocol()).isEqualTo("tcp");
        assertThat(first.service()).isEqualTo("http");
        assertThat(first.trueLabel()).isEqualTo("Normal");
        assertThat(first.features()).containsEntry("flow_duration", 1.5);
        assertThat(first.features()).containsEntry("fwd_pkts_tot", 10.0);
        assertThat(first.features()).doesNotContainKey("Attack_type");
        assertThat(first.features()).doesNotContainKey("proto");
    }
}
