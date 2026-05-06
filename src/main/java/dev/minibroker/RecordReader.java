package dev.minibroker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class RecordReader {

    private final FileChannel channel;

    public RecordReader(Path path) throws IOException {
        this.channel = FileChannel.open(path, StandardOpenOption.READ);
    }

    public List<Record> readAll() throws IOException {
        List<Record> records = new ArrayList<>();

        while (channel.position() < channel.size()) {
            // 1. magic check
            ByteBuffer magicBuf = ByteBuffer.allocate(2);
            channel.read(magicBuf);
            magicBuf.flip();
            short magic = magicBuf.getShort();

            if (magic != (short) 0xCAFE) {
                break;
            }

            // 2. read offset, timestamp
            ByteBuffer fixed = ByteBuffer.allocate(16);
            channel.read(fixed);
            fixed.flip();
            long offset = fixed.getLong();
            long timestamp = fixed.getLong();

            // 3. read key
            ByteBuffer keyLengthBuf = ByteBuffer.allocate(2);
            channel.read(keyLengthBuf);
            keyLengthBuf.flip();
            short keyLength = keyLengthBuf.getShort();
            byte[] key = null;
            if (keyLength != -1) {
                key = new byte[keyLength];
                channel.read(ByteBuffer.wrap(key));
            }

            // 4. read value
            ByteBuffer valueLengthBuf = ByteBuffer.allocate(2);
            channel.read(valueLengthBuf);
            valueLengthBuf.flip();
            short valueLength = valueLengthBuf.getShort();
            byte[] value = null;
            if (valueLength != -1) {
                value = new byte[valueLength];
                channel.read(ByteBuffer.wrap(value));
            }

            records.add(new Record(offset, timestamp, key, value));

        }

        return records;
    }

    public void close() throws IOException {
        channel.close();
    }
}
