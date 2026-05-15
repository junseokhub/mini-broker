package dev.minibroker.util;

import dev.minibroker.log.record.Record;
import dev.minibroker.log.record.RecordReader;

import java.nio.file.Path;
import java.util.List;

public class LogDump {
    public static void main(String[] args) throws Exception {
        Path logPath = Path.of("data/orders.log");
        try (RecordReader reader = new RecordReader(logPath)) {
            List<Record> records = reader.readAll();
            for (Record r : records) {
                System.out.printf("offset=%-5d key=%-10s value=%s%n",
                        r.offset(),
                        new String(r.key()),
                        new String(r.value()));
            }
        }
    }
}