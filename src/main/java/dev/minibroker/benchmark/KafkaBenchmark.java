package dev.minibroker.benchmark;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.openjdk.jmh.annotations.*;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class KafkaBenchmark {

    private KafkaProducer<byte[], byte[]> producer;
    private byte[] key;
    private byte[] value;

    @Setup
    public void setup() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9093");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put(ProducerConfig.ACKS_CONFIG, "1");

        producer = new KafkaProducer<>(props);
        key   = "key-test".getBytes();
        value = "value-test-payload".getBytes();
    }

    @TearDown
    public void teardown() {
        producer.close();
    }

    @Benchmark
    public void sendMessage() {
        producer.send(new ProducerRecord<>("bench-topic", key, value));
    }
}