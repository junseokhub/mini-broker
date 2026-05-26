package dev.minibroker.benchmark;

import dev.minibroker.log.segment.LogSegment;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class AppendBenchmark {

    private LogSegment segment;
    private Path tempDir;

    @Setup
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("jmh-bench");
        segment = new LogSegment(
                tempDir.resolve("bench.log"),
                tempDir.resolve("bench.index"),
                100
        );
    }

    @TearDown
    public void teardown() throws IOException {
        segment.close();
    }

    @Benchmark
    public long appendMessage() throws IOException {
        return segment.append(
                "key-test".getBytes(),
                "value-test-payload".getBytes()
        );
    }
}