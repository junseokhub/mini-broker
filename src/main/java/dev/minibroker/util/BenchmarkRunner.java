package dev.minibroker.util;

import dev.minibroker.benchmark.AppendBenchmark;
import dev.minibroker.benchmark.KafkaBenchmark;
import dev.minibroker.benchmark.ProducerBenchmark;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BenchmarkRunner {
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(AppendBenchmark.class.getSimpleName())
                .include(ProducerBenchmark.class.getSimpleName())
                .include(KafkaBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}