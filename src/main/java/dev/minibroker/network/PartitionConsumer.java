package dev.minibroker.network;

import dev.minibroker.log.index.IndexReader;
import dev.minibroker.log.record.Record;
import dev.minibroker.log.record.RecordReader;
import dev.minibroker.log.segment.LogSegment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PartitionConsumer {

    private final int partitionIndex;
    private final LogSegment segment;
    private final ConsumerGroup group;

    public PartitionConsumer(int partitionIndex, LogSegment segment, ConsumerGroup group) {
        this.partitionIndex = partitionIndex;
        this.segment = segment;
        this.group = group;
    }

    public List<Record> poll() throws IOException {
        // 1. 그룹에서 이 파티션의 마지막 커밋 offset 가져오기
        long committedOffset = group.committedOffset(partitionIndex);

        // 2. IndexReader로 position 찾기
        Path indexPath = segment.indexPath();
        long position;
        try (IndexReader indexReader = new IndexReader(indexPath)) {
            position = indexReader.findPosition(committedOffset);
        }

        // 3. position부터 읽기
        Path logPath = segment.logPath();
        List<Record> records;
        try (RecordReader reader = new RecordReader(logPath)) {
            records = reader.readFrom(position)
                    .stream()
                    .filter(r -> r.offset() >= committedOffset)
                    .toList();
        }
        // 4. 읽은 메시지 중 마지막 offset + 1 커밋
        if (!records.isEmpty()) {
            long lastOffset = records.getLast().offset();
            group.commitOffset(partitionIndex, lastOffset + 1);
        }

        return records;
    }

    public int partitionIndex() { return partitionIndex; }
}