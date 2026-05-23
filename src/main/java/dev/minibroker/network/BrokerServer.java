package dev.minibroker.network;

import dev.minibroker.log.index.IndexReader;
import dev.minibroker.log.record.Record;
import dev.minibroker.log.record.RecordReader;
import dev.minibroker.log.segment.LogSegment;
import dev.minibroker.log.segment.Topic;

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
    private final Map<String, Topic> topics = new HashMap<>();

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

        // Topic 가져오기 (없으면 파티션 3개로 생성)
        Topic t = topics.computeIfAbsent(topic, name -> {
            try {
                return new Topic(name, 3, dataDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // 파티션 선택 후 append (offset은 LogSegment가 직접 부여)
        int partitionIndex = t.selectPartition(key);
        LogSegment segment = t.partition(partitionIndex);
        long offset = segment.append(key, value);


        // offset + partitionIndex 응답
        ByteBuffer response = ByteBuffer.allocate(8 + 4);
        response.putLong(offset);
        response.putInt(partitionIndex);
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
        String topicName = new String(topicBytes);

        // 2. 파티션 번호 읽기 ← 추가
        ByteBuffer partitionBuf = ByteBuffer.allocate(4);
        client.read(partitionBuf);
        partitionBuf.flip();
        int partition = partitionBuf.getInt();

        // 3. 시작 offset 읽기
        ByteBuffer offsetBuf = ByteBuffer.allocate(8);
        client.read(offsetBuf);
        offsetBuf.flip();
        long offset = offsetBuf.getLong();

        // 4. Topic에서 해당 파티션 가져오기 ← 변경
        Topic t = topics.get(topicName);
        LogSegment segment = t.partition(partition);
        Path indexPath = segment.indexPath();
        Path logPath = segment.logPath();

        // 5. IndexReader로 position 찾기
        long position;
        try (IndexReader indexReader = new IndexReader(indexPath)) {
            position = indexReader.findPosition(offset);
        }

        // 6. position부터 읽어서 응답 (기존 코드 동일)
        try (RecordReader recordReader = new RecordReader(logPath)) {
            List<Record> records = recordReader.readFrom(position);

            ByteBuffer countBuf = ByteBuffer.allocate(4);
            countBuf.putInt(records.size());
            countBuf.flip();
            client.write(countBuf);

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