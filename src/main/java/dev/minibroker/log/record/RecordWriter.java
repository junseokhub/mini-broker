package dev.minibroker.log.record;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class RecordWriter implements AutoCloseable {

    private final FileChannel channel;

    public RecordWriter(Path path) throws IOException {
        this.channel = FileChannel.open(path,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND,
                StandardOpenOption.CREATE);
    }

    // kafka 스타일 바이너리 append
    public long append(Record record) throws IOException {
        long position = channel.position();
        // 고정 바이트 총 합
        int keyLength   = (record.key()   == null) ? 0 : record.key().length;
        int valueLength = (record.value() == null) ? 0 : record.value().length;

        int size = 26 + keyLength + valueLength;   // 2+8+8+4+4 = 26

        ByteBuffer buffer = ByteBuffer.allocate(size); // 새로운 공간
        buffer.putShort((short) 0xCAFE);
        buffer.putLong(record.offset());
        buffer.putLong(record.timestamp());

        if (record.key() == null) {
            buffer.putInt(-1);
        } else {
            buffer.putInt(record.key().length);
            buffer.put(record.key());
        }
        if (record.value() == null) {
            buffer.putInt(-1);
        } else {
            buffer.putInt(record.value().length);
            buffer.put(record.value());
        }

        buffer.flip();
        channel.write(buffer);
        return position;
    }

    public void close() throws IOException {
        channel.close();
    }
}
