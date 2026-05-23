package dev.minibroker.network;

import dev.minibroker.log.record.Record;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class ConsumerClient implements AutoCloseable {

    private final SocketChannel channel;

    public ConsumerClient(String host, int port) throws IOException {
        this.channel = SocketChannel.open(new InetSocketAddress(host, port));
    }

    public List<Record> fetch(String topic, int partition, long fromOffset) throws IOException {
        // 1. FETCH 요청 전송
        // [type: 0x02][토픽명 길이: 2B][토픽명: NB][offset: 8B]
        byte[] topicBytes = topic.getBytes();
        ByteBuffer buf = ByteBuffer.allocate(1 + 2 + topicBytes.length + 8);
        buf.put((byte) 0x02);
        buf.putShort((short) topicBytes.length);
        buf.put(topicBytes);
        buf.putInt(partition);
        buf.putLong(fromOffset);
        buf.flip();
        channel.write(buf);

        // 2. 응답 수신
        // 메세지 수 읽기
        ByteBuffer countBuf = ByteBuffer.allocate(4);
        channel.read(countBuf);
        countBuf.flip();
        int count = countBuf.getInt();

        List<Record> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // offset
            ByteBuffer offsetBuf = ByteBuffer.allocate(8);
            channel.read(offsetBuf);
            offsetBuf.flip();
            long offset = offsetBuf.getLong();

            // key
            ByteBuffer keyLenBuf = ByteBuffer.allocate(2);
            channel.read(keyLenBuf);
            keyLenBuf.flip();
            short keyLen = keyLenBuf.getShort();
            byte[] key = null;
            if (keyLen != -1) {
                key = new byte[keyLen];
                channel.read(ByteBuffer.wrap(key));
            }

            // value
            ByteBuffer valueLenBuf = ByteBuffer.allocate(2);
            channel.read(valueLenBuf);
            valueLenBuf.flip();
            short valueLen = valueLenBuf.getShort();
            byte[] value = null;
            if (valueLen != -1) {
                value = new byte[valueLen];
                channel.read(ByteBuffer.wrap(value));
            }

            records.add(new Record(offset, 0L, key, value));
        }
        return records;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}