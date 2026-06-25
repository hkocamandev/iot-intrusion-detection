package com.github.hkocamandev.iot_intrusion_detection.consumer;

import com.github.hkocamandev.iot_intrusion_detection.TestcontainersConfiguration;
import com.github.hkocamandev.iot_intrusion_detection.dto.AlertEvent;
import com.github.hkocamandev.iot_intrusion_detection.model.AttackType;
import com.github.hkocamandev.iot_intrusion_detection.model.DetectionSource;
import com.github.hkocamandev.iot_intrusion_detection.model.Severity;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.test.context.TestPropertySource;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Import({TestcontainersConfiguration.class, AlertDlqIT.DltCapture.class})
@TestPropertySource(properties = {"app.ml.enabled=false", "app.llm.enabled=false"})
class AlertDlqIT {

    private static final String POISON_SOURCE_ID = "poison-alert-dlt-probe";

    /** Raw-string listener on iot.alerts-dlt to capture dead-letter records. */
    @Component
    static class DltCapture {

        final BlockingQueue<ConsumerRecord<String, String>> received = new LinkedBlockingQueue<>();

        @KafkaListener(
                topics = "${app.topics.alerts}-dlt",
                groupId = "dlt-capture-group",
                properties = {
                        "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                        "auto.offset.reset=earliest"
                })
        void onDlt(ConsumerRecord<String, String> record) {
            received.add(record);
        }
    }

    @Autowired KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired DltCapture dltCapture;
    @Autowired ConsumerFactory<String, Object> consumerFactory;

    @MockitoSpyBean AlertConsumer alertConsumer;

    @Value("${app.topics.alerts}")
    String alertsTopic;

    /** Lazily-created raw Kafka producer; closed after each test. */
    private KafkaProducer<String, String> rawProducer;

    @BeforeEach
    void setUpRawProducer() {
        // Derive bootstrap servers from the ConsumerFactory config so the producer
        // connects to the same (Testcontainers-provided) broker as the consumers.
        Object servers = consumerFactory.getConfigurationProperties()
                .get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
        String bootstrapServers = servers instanceof List<?> list
                ? String.join(",", list.stream().map(Object::toString).toList())
                : String.valueOf(servers);

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        rawProducer = new KafkaProducer<>(props);
    }

    @AfterEach
    void tearDownRawProducer() {
        if (rawProducer != null) {
            rawProducer.close();
        }
    }

    @Test
    void poisonMessageIsRoutedToDeadLetterTopic() throws Exception {
        // Make the consumer throw deterministically for the poison marker
        doThrow(new RuntimeException("simulated processing failure"))
                .when(alertConsumer)
                .onAlert(argThat(e -> POISON_SOURCE_ID.equals(e.sourceId())));

        AlertEvent poison = new AlertEvent(
                Instant.now(),
                POISON_SOURCE_ID,
                DetectionSource.RULE,
                AttackType.DOS,
                Severity.HIGH,
                9.9,
                "dos-high-packet-rate",
                "tcp",
                "http",
                "DOS_SYN_Hping");

        kafkaTemplate.send(alertsTopic, POISON_SOURCE_ID, poison);

        // After all retries are exhausted (attempts=3 → 2 retries), message must land in DLT
        await()
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> {
                    ConsumerRecord<String, String> dltRecord =
                            dltCapture.received.poll(0, TimeUnit.SECONDS);
                    assertThat(dltRecord).as("Expected a record in iot.alerts-dlt").isNotNull();
                    assertThat(dltRecord.value()).contains(POISON_SOURCE_ID);
                });
    }

    @Test
    void nonDeserializablePayloadIsRoutedToDeadLetterTopic() throws Exception {
        // Send a raw string that cannot be deserialized as AlertEvent.
        // ErrorHandlingDeserializer wraps the failure: the container invokes
        // the listener with null and the @RetryableTopic error handler routes it
        // to the DLT after exhausting retries.
        String garbledPayload = "this-is-not-valid-alertevent-json";

        // Use a synchronous Kafka producer to ensure delivery before asserting.
        rawProducer.send(new ProducerRecord<>(alertsTopic, "deser-poison-key", garbledPayload)).get(10, TimeUnit.SECONDS);

        await()
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> {
                    ConsumerRecord<String, String> dltRecord =
                            dltCapture.received.poll(0, TimeUnit.SECONDS);
                    assertThat(dltRecord).as("Expected garbled record in iot.alerts-dlt").isNotNull();
                });
    }
}
