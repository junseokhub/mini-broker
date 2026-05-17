package dev.minibroker.network;

import dev.minibroker.log.index.IndexReader;
import dev.minibroker.log.record.Record;
import dev.minibroker.log.record.RecordReader;
import dev.minibroker.log.segment.LogSegment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
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
        // 1. 토픽명 읽기
        ByteBuffer topicBuf = ByteBuffer.allocate(2);
        client.read(topicBuf);
        topicBuf.flip();
        short topicLen = topicBuf.getShort();
        byte[] topicBytes = new byte[topicLen];
        client.read(ByteBuffer.wrap(topicBytes));
        String topic = new String(topicBytes);

        // 2. 시작 offset 읽기
        ByteBuffer offsetBuf = ByteBuffer.allocate(8);
        client.read(offsetBuf);
        offsetBuf.flip();
        long offset = offsetBuf.getLong();

        // 3. IndexReader로 position 찾기
        Path indexPath = dataDir.resolve(topic + ".index");
        long position;
        try (IndexReader indexReader = new IndexReader(indexPath)) {
            position = indexReader.findPosition(offset); // fetchOffset → offset
        }

        // 4. position부터 읽어서 응답
        Path logPath = dataDir.resolve(topic + ".log");
        try (RecordReader recordReader = new RecordReader(logPath)) {
            List<Record> records = recordReader.readFrom(position);

            // 메시지 수 먼저 응답
            ByteBuffer countBuf = ByteBuffer.allocate(4);
            countBuf.putInt(records.size());
            countBuf.flip();
            client.write(countBuf);

            // 각 메시지 응답
            for (Record r : records) {
                int keyLen   = (r.key()   == null) ? 0 : r.key().length;
                int valueLen = (r.value() == null) ? 0 : r.value().length;
                ByteBuffer buf = ByteBuffer.allocate(8 + 2 + keyLen + 2 + valueLen);
                buf.putLong(r.offset());
                buf.putShort((short)(r.key()   == null ? -1 : keyLen));
                if (r.key() != null) buf.put(r.key());
                buf.putShort((short)(r.value() == null ? -1 : valueLen));
                if (r.value() != null) buf.put(r.value());
                buf.flip();
                client.write(buf);
            }
        }
    }
}