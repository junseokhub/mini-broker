package dev.minibroker.network;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ProducerClient implements AutoCloseable {

    private final SocketChannel channel;

    public ProducerClient(String host, int port) throws IOException {
        this.channel = SocketChannel.open(new InetSocketAddress(host, port));
    }

    public ProduceResult send(String topic, byte[] key, byte[] value) throws IOException {
        // [전체 길이: 4B][토픽명 길이: 2B][토픽명: NB][key 길이: 2B][key: NB][value 길이: 2B][value: NB]

        byte[] topicBytes = topic.getBytes();
        int keyLen = (key == null) ? 0 : key.length;
        int valueLen = (value == null) ? 0 : value.length;

        int totalLen = 2 + topicBytes.length + 2 + keyLen + 2 + valueLen;

        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + totalLen);
        buffer.put((byte) 0x01); // PRODUCE
        buffer.putInt(totalLen);
        buffer.putShort((short) topicBytes.length);
        buffer.put(topicBytes);

        if (key == null) {
            buffer.putShort((short) -1);
        } else {
            buffer.putShort((short) key.length);
            buffer.put(key);
        }
        if (value == null) {
            buffer.putShort((short) -1);
        } else {
            buffer.putShort((short) value.length);
            buffer.put(value);
        }


        buffer.flip();
        channel.write(buffer);

        // offset + partition 수신
        ByteBuffer response = ByteBuffer.allocate(12);
        channel.read(response);
        response.flip();
        long offset = response.getLong();
        int partition = response.getInt();
        return new ProduceResult(offset, partition);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
