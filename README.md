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

## Build

```bash
./gradlew build
```

## Run

```bash
./gradlew run
```