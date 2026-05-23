package dev.minibroker.util;

import dev.minibroker.log.record.Record;
import dev.minibroker.network.ConsumerClient;

public class ConsumerRunner {
    public static void main(String[] args) throws Exception {
        try (ConsumerClient consumer = new ConsumerClient("localhost", 9092)) {
            var records = consumer.fetch("orders", 0, 0L);
            for (Record r : records) {
                System.out.printf("offset=%-5d key=%-10s value=%s%n",
                        r.offset(),
                        new String(r.key()),
                        new String(r.value()));
            }
        }
    }
}