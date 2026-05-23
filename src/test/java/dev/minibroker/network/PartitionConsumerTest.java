package dev.minibroker.network;

import dev.minibroker.log.record.Record;
import dev.minibroker.log.segment.LogSegment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PartitionConsumerTest {

    @TempDir
    Path tempDir;

    @Test
    void pollReadsFromCommittedOffset() throws Exception {
        Path logPath   = tempDir.resolve("test-0.log");
        Path indexPath = tempDir.resolve("test-0.index");

        // 1. LogSegment에 5개 write
        try (LogSegment segment = new LogSegment(logPath, indexPath, 100)) {
            for (int i = 0; i < 5; i++) {
                segment.append(("key-" + i).getBytes(), ("value-" + i).getBytes());
            }

            // 2. ConsumerGroup + PartitionConsumer 생성
            ConsumerGroup group = new ConsumerGroup("test-group", "test");
            PartitionConsumer consumer = new PartitionConsumer(0, segment, group);

            // 3. 첫 번째 poll
            List<Record> first = consumer.poll();
            assertEquals(5, first.size());

            // 4. 두 번째 poll
            List<Record> second = consumer.poll();
            assertEquals(0, second.size());
        }
    }
}