package dev.minibroker;

import dev.minibroker.log.record.Record;
import dev.minibroker.log.record.RecordReader;
import dev.minibroker.network.BrokerServer;
import dev.minibroker.network.ProducerClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BrokerIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void producerSendsMessageAndBrokerSavesToLog() throws Exception {
        // 1. 서버를 별도 스레드로
        BrokerServer server = new BrokerServer(19092, tempDir);
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // 테스트 끝나면 자동 종료 옵션
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(1000);

        // 2. Producer로 메시지 전송
        try (ProducerClient producer = new ProducerClient("localhost", 19092)) {
            long offset = producer.send(
                    "orders",
                    "key-0".getBytes(),
                    "value-0".getBytes()
            );
            assertEquals(0L, offset);
        }

        // 3. 로그 파일 읽어서 검증
        byte[] key = "key-0".getBytes();
        int partitionIndex = Math.abs(Arrays.hashCode(key) % 3);
        Path logPath = tempDir.resolve("orders-" + partitionIndex + ".log");

        try (RecordReader reader = new RecordReader(logPath)) {
            List<Record> records = reader.readAll();
            assertEquals(1, records.size());
            assertArrayEquals("key-0".getBytes(), records.getFirst().key());
            assertArrayEquals("value-0".getBytes(), records.getFirst().value());
        }
    }
}
