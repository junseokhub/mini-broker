package dev.minibroker.util;

import dev.minibroker.network.ProduceResult;
import dev.minibroker.network.ProducerClient;

public class ProducerRunner {
    public static void main(String[] args) throws Exception {
        try (ProducerClient producer = new ProducerClient("localhost", 9092)) {
            for (int i = 0; i < 5; i++) {
                ProduceResult result = producer.send(
                        "orders",
                        ("key-" + i).getBytes(),
                        ("value-" + i).getBytes()
                );
                System.out.printf("sent → offset=%d partition=%d%n", result.offset(), result.partition());
            }
        }
    }
}