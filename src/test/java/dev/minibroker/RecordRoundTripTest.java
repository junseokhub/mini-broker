package dev.minibroker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

public class RecordRoundTripTest {

    @TempDir
    Path tempDir;

    @Test
    void hundredRecords() throws Exception {
        Path logPath = tempDir.resolve("log.bin");

        try (RecordWriter writer = new RecordWriter(logPath)) {
            for (int i = 0; i < 100; i++) {
                writer.append(
                        new Record(i, System.currentTimeMillis(), ("key-" + i).getBytes(), ("value-" + i).getBytes())
                );
            }
        }
    }
}
