package dev.minibroker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

        try (RecordReader reader = new RecordReader(logPath)) {
            List<Record> records = reader.readAll();

            assertEquals(100, records.size());

            for (int i = 0; i < 100; i++) {
                Record r = records.get(i);
                assertEquals(i , r.offset());
                assertArrayEquals(("key-" + i).getBytes(), r.key());
                assertArrayEquals(("value-" + i).getBytes(), r.value());
            }
        }
    }
}
