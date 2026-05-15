package dev.minibroker.network;

import dev.minibroker.log.segment.LogSegment;
import dev.minibroker.log.record.Record;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class BrokerServer {

    private final int port;
    private final Path dataDir;
    private final Map<String, LogSegment> segments = new HashMap<>();
    private final Map<String, Long> offsets = new HashMap<>();

    public BrokerServer(int port, Path dataDir) {
        this.port = port;
        this.dataDir = dataDir;
    }

    public void start() throws IOException {
        Selector selector = Selector.open();

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false); // non-blocking
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();

            for (SelectionKey key : selector.selectedKeys()) {
                if (key.isAcceptable()) {
                    SocketChannel client = serverChannel.accept();
                    client.configureBlocking(false);

                    // 클라이언트는 데이터 읽을 준비 완료된 상태
                    client.register(selector, SelectionKey.OP_READ);

                } else if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();

                    // 1. 전체 길이 읽기
                    ByteBuffer lenBuf = ByteBuffer.allocate(4);
                    int bytesRead = client.read(lenBuf);
                    if (bytesRead < 4) continue;
                    lenBuf.flip();
                    int totalLen = lenBuf.getInt();

                    // 2. 나머지 페이로드 읽기
                    ByteBuffer payload = ByteBuffer.allocate(totalLen);
                    client.read(payload);
                    payload.flip();

                    // 3. 토픽명 읽기
                    short topicLen = payload.getShort();
                    byte[] topicBytes = new byte[topicLen];
                    payload.get(topicBytes);
                    String topic = new String(topicBytes);

                    // 4. key 읽기
                    short keyLen = payload.getShort();
                    byte[] keys = null;
                    if (keyLen != -1) {
                        keys = new byte[keyLen];
                        payload.get(keys);
                    }

                    // 5. value
                    short valueLen = payload.getShort();
                    byte[] value = null;
                    if (valueLen != -1) {
                        value = new byte[valueLen];
                        payload.get(value);
                    }

                    // 6. LogSegment에 append
                    LogSegment segment = segments.computeIfAbsent(topic, t -> {
                        try {
                            Path logPath   = dataDir.resolve(t + ".log");
                            Path indexPath = dataDir.resolve(t + ".index");
                            return new LogSegment(logPath, indexPath, 100);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    long offset = offsets.getOrDefault(topic, 0L);
                    Record record = new Record(offset, System.currentTimeMillis(), keys, value);
                    segment.append(record);
                    offsets.put(topic, offset + 1);

                    // 7. offset을 Producer에게 응답
                    ByteBuffer response = ByteBuffer.allocate(8);
                    response.putLong(offset);
                    response.flip();
                    client.write(response);

                }
                selector.selectedKeys().clear();
            }
        }
    }
}
