package dev.minibroker.network;

import dev.minibroker.log.record.Record;
import dev.minibroker.log.segment.LogSegment;

import java.io.IOException;
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
        return List.of();
    }

    public int partitionIndex() { return partitionIndex; }
}