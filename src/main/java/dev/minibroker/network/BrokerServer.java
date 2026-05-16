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
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();

            for (SelectionKey key : selector.selectedKeys()) {
                if (key.isAcceptable()) {
                    SocketChannel client = serverChannel.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);

                } else if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();

                    ByteBuffer typeBuf = ByteBuffer.allocate(1);
                    int bytesRead = client.read(typeBuf);
                    if (bytesRead < 1) continue;
                    typeBuf.flip();
                    byte type = typeBuf.get();

                    if (type == 0x01) {
                        handleProduce(client);
                    } else if (type == 0x02) {
                        handleFetch(client);
                    }
                }
                selector.selectedKeys().clear();
            }
        }
    }

    private void handleProduce(SocketChannel client) throws IOException {
        // 전체 길이 읽기
        ByteBuffer lenBuf = ByteBuffer.allocate(4);
        client.read(lenBuf);
        lenBuf.flip();
        int totalLen = lenBuf.getInt();

        // 페이로드 읽기
        ByteBuffer payload = ByteBuffer.allocate(totalLen);
        client.read(payload);
        payload.flip();

        // 토픽명
        short topicLen = payload.getShort();
        byte[] topicBytes = new byte[topicLen];
        payload.get(topicBytes);
        String topic = new String(topicBytes);

        // key
        short keyLen = payload.getShort();
        byte[] key = null;
        if (keyLen != -1) {
            key = new byte[keyLen];
            payload.get(key);
        }

        // value
        short valueLen = payload.getShort();
        byte[] value = null;
        if (valueLen != -1) {
            value = new byte[valueLen];
            payload.get(value);
        }

        // LogSegment에 append
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
        segment.append(new Record(offset, System.currentTimeMillis(), key, value));
        offsets.put(topic, offset + 1);

        // offset 응답
        ByteBuffer response = ByteBuffer.allocate(8);
        response.putLong(offset);
        response.flip();
        client.write(response);
    }

    private void handleFetch(SocketChannel client) throws IOException {
    }
}