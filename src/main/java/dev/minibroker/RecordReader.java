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

        // 1. magic check
        while (channel.position() < channel.size()) {
            ByteBuffer magicBuf = ByteBuffer.allocate(2);
            channel.read(magicBuf);
            magicBuf.flip();
            short magic = magicBuf.getShort();

            if (magic != (short) 0xCAFE) {
                break;
            }
        }

        // 2. read offset, timestamp
        return records;
    }
}
