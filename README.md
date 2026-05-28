# mini-broker

A single-node, Kafka-like message broker built from scratch in Java.

> **Goal**: Understand *why* Kafka is designed the way it is — by building
> a simplified version of it.

## Why

I operate a production Strimzi Kafka cluster (3-broker KRaft). But
*operating* Kafka and *understanding* Kafka are two different things.
This project closes that gap.

Along the way: hands-on experience with NIO, binary protocols,
append-only logs, and the trade-offs that make Kafka fast.

## Steps

| Stage | Goal | Status |
|-------|------|--------|
| 1 | Append-only log on disk | ⬜ |
| 2 | Sparse index + segment rolling | ⬜ |
| 3 | NIO TCP server + binary protocol | ⬜ |
| 4 | Producer / Consumer client library | ⬜ |
| 5 | Partitions + consumer groups | ⬜ |
| 6 | Benchmarks vs Apache Kafka | ⬜ |

## Requirements

- Java 21
- Gradle Kotlin DSL (wrapper included)
- Docker + Docker Compose (Kafka compare)

## Build

```bash
./gradlew build
```

## Run

```bash
./gradlew run
```

## Benchmark

### mini-broker

IntelliJ에서 `BenchmarkRunner.main()` 실행.

```
Benchmark                       Mode  Cnt       Score       Error  Units
AppendBenchmark.appendMessage  thrpt    5  542893.757 ± 49598.545  ops/s
```

### vs Apache Kafka

```bash
# Run Kafka
docker-compose up -d
 
# create bench-topic 
docker exec -it mini-broker-kafka-1 kafka-topics --create \
  --topic bench-topic \
  --bootstrap-server localhost:9093 \
  --partitions 1 \
  --replication-factor 1
 
# BenchmarkRunner Run (IntelliJ)
 
# EXIT
docker-compose down
```

## Ports

| Service               | Port  |
|-----------------------|-------|
| mini-broker           | 9092  |
| Kafka (Docker)        | 9093  |
| BrokerIntegrationTest | 19092 |


## Test

```bash
./gradlew test
```

**RecordRoundTripTest** — RecordWriter + RecordReader unit test
- Write 100 messages → read back → verify order and content
- Checks that `offset`, `key`, `value` are correctly restored
- Result: ✅ PASS
  **LogSegmentTest** — sparse index behavior
- Write 300 messages (indexInterval=100)
- Find position via `IndexReader.findPosition(150)`
- Read from that position and verify offset 150 message
- Result: ✅ PASS
  **BrokerIntegrationTest** — end-to-end integration test
- Start `BrokerServer` on a separate thread (port 19092)
- Send message via `ProducerClient`
- Read log file directly and verify message was stored
- Result: ✅ PASS
  **PartitionConsumerTest** — consumer group offset commit
- Write 5 messages to `LogSegment`
- First `poll()` → returns 5 records
- Second `poll()` → returns 0 records (offset already committed)
- Result: ✅ PASS
---

## Producer / Consumer

Start the broker first, then run each Runner from IntelliJ.

**ProducerRunner output:**
```
sent → offset=0 partition=0
sent → offset=0 partition=1
sent → offset=0 partition=2
sent → offset=1 partition=0
sent → offset=1 partition=1
```

Messages are distributed across partitions via key-based hashing. Each partition manages its own offset independently.

**ConsumerRunner output:**
```
# consumer.fetch("orders", 0, 0L) — topic, partition, fromOffset
offset=0     key=key-0      value=value-0
offset=1     key=key-3      value=value-3
```
 
---

## LogDump (debug utility)

Prints log file contents in human-readable format.

```java
// LogDump.java — update path before running
Path logPath = Path.of("data/orders-0.log");
```

Output:
```
offset=0     key=key-0      value=value-0
offset=1     key=key-3      value=value-3
```