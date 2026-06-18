package dev.minibroker.log.segment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Topic {

    private final String name;
    private final List<LogSegment> partitions;
    private int roundRobinCounter = 0;

    public Topic(String name, int partitionCount, Path dataDir) throws IOException {
        this.name = name;
        this.partitions = new ArrayList<>();
        for (int i = 0; i < partitionCount; i++) {
            Path logPath   = dataDir.resolve(name + "-" + i + ".log");
            Path indexPath = dataDir.resolve(name + "-" + i + ".index");
            partitions.add(new LogSegment(logPath, indexPath, 100));
        }
    }

    public int selectPartition(byte[] key) {
        if (key == null) {
            return Math.floorMod(roundRobinCounter++, partitions.size());
        }
        return Math.floorMod(Arrays.hashCode(key), partitions.size());
    }

    public LogSegment partition(int index) {
        return partitions.get(index);
    }

    public int partitionCount() {
        return partitions.size();
    }

    public String name() {
        return name;
    }
}