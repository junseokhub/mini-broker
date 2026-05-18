package dev.minibroker.network;

import java.util.HashMap;
import java.util.Map;

public class ConsumerGroup {

    private final String groupId;
    private final String topic;
    private final Map<Integer, Long> committedOffsets = new HashMap<>();

    public ConsumerGroup(String groupId, String topic) {
        this.groupId = groupId;
        this.topic = topic;
    }

    public long committedOffset(int partition) {
        return committedOffsets.getOrDefault(partition, 0L);
    }

    public void commitOffset(int partition, long offset) {
        committedOffsets.put(partition, offset);
    }

    public String groupId() { return groupId; }
    public String topic()   { return topic; }
}