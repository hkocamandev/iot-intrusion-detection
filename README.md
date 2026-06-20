# IoT Network Intrusion Detection

A Kafka-based streaming system that performs near real-time intrusion detection on
RT-IoT2022 network traffic. Built with Spring Boot producers/consumers, PostgreSQL,
rule-based and ML-based anomaly detection, a REST API and a live dashboard.

## Architecture (Phase 1 — ingest & store)

```
CSV  ->  Kafka (iot.traffic.raw)  ->  StorageConsumer  ->  PostgreSQL
```

A `CsvReplaySimulator` reads the RT-IoT2022 CSV schema-agnostically and publishes each
flow as a `TrafficEvent` to the `iot.traffic.raw` topic (keyed by source). `StorageConsumer`
consumes the topic and persists each flow into the `network_flows` table.

## Phase 2 — Rule-based detection

```
                         ┌─ StorageConsumer (group: storage)        -> network_flows
iot.traffic.raw  ────────┤
                         └─ RuleBasedConsumer (group: rule-engine)  -> iot.alerts
                                                                          │
                                              AlertConsumer (group: alert-processor) -> alerts
                                              (retry + dead-letter: iot.alerts-dlt)
```

Rules are config-driven thresholds defined under `app.rules` in `application.yml`, so new
detections are added without code changes. Because `rule-engine` is a separate consumer
group from `storage`, both consumers independently receive every traffic message.

## Phase 3 — ML-based detection

```
                ┌─ StorageConsumer    (group: storage)     -> network_flows
iot.traffic.raw ┼─ RuleBasedConsumer  (group: rule-engine) -> iot.alerts (RULE)
                └─ MlBasedConsumer    (group: ml-engine)   -> ml-service /predict
                                                                 │  attack_type != NORMAL
                                                                 ▼
                                                             iot.alerts (ML)
```

A Python FastAPI service (`ml-service/`) serves an XGBoost classifier trained offline
(`train.py`, on RT-IoT2022 if present, otherwise on synthetic labeled data). `MlBasedConsumer`
reads `iot.traffic.raw` under its own consumer group, calls the model over REST with a timeout,
and publishes `detectionSource=ML` alerts. If the ML service is unavailable the call falls back
silently — storage and rule-based detection are unaffected. ML can be toggled with `app.ml.enabled`.

Run the ML service with `docker compose up -d --build ml-service` (or locally: `cd ml-service &&
pip install -r requirements.txt && python train.py && uvicorn app:app --port 8000`).

## Tech stack

- Java 21, Spring Boot 4.1
- Apache Kafka (Confluent, KRaft mode)
- PostgreSQL 16 (JSONB feature storage)
- Testcontainers for integration tests

## Running locally

```bash
docker compose up -d        # kafka, kafka-ui (:8085), postgres (:5432)
./mvnw spring-boot:run      # application (:8080)
```

To replay the dataset, see `data/README.md`, then set `app.simulator.enabled=true`.

## Tests

```bash
./mvnw test
```

Integration tests spin up real Kafka and PostgreSQL containers via Testcontainers,
so Docker must be running.

## Notes

Implementation and learning notes are maintained locally and are intentionally excluded
from version control.
