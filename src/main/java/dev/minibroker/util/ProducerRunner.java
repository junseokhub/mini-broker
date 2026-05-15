package dev.minibroker.util;

import dev.minibroker.network.ProducerClient;

public class ProducerRunner {
    public static void main(String[] args) throws Exception {
        try (ProducerClient producer = new ProducerClient("localhost", 9092)) {
            for (int i = 0; i < 5; i++) {
                long offset = producer.send(
                        "orders",
                        ("key-" + i).getBytes(),
                        ("value-" + i).getBytes()
                );
                System.out.println("sent → offset=" + offset);
            }
        }
    }
}