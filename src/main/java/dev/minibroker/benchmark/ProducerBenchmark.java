package dev.minibroker.benchmark;

import dev.minibroker.network.ProducerClient;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class ProducerBenchmark {

    private ProducerClient producer;
    private byte[] key;
    private byte[] value;

    @Setup
    public void setup() throws Exception {
        producer = new ProducerClient("localhost", 9092);
        key   = "key-test".getBytes();
        value = "value-test-payload".getBytes();
    }

    @TearDown
    public void teardown() throws Exception {
        producer.close();
    }

    @Benchmark
    public void sendMessage() throws Exception {
        producer.send("bench-topic", key, value);
    }
}