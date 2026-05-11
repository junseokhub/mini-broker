package dev.minibroker;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LogSegmentTest {

    @Test
    void findPositionAndRead() throws Exception {
        Path logPath = Path.of("test.log");
        Path indexPath = Path.of("test.index");

        // 우선 300개 write
        try (LogSegment logSegment = new LogSegment(logPath, indexPath, 100)) {
            for (int i = 0; i < 300; i++) {
                logSegment.append(new Record(
                        i, System.currentTimeMillis(), ("key-" + i).getBytes(), ("value-" + i).getBytes()
                ));
            }
        }

        // offset 150 증가
        try (IndexReader indexReader = new IndexReader(indexPath)) {
            long position = indexReader.findPosition(150);

            // position이 올바른지 확인
            assertTrue(position > 0);

            // 찾은 position에서부터 읽어와서 offset 150 검증
            try (RecordReader recordReader = new RecordReader(logPath)) {
                List<Record> records = recordReader.readAll();
                Record target = records.stream()
                .filter(r -> r.offset() == 150)
                .findFirst()
                .orElseThrow();

                assertArrayEquals(("key-150").getBytes(), target.key());
            }
        }
    }
}
