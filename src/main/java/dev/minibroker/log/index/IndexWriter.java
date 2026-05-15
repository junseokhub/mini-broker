package dev.minibroker.log.index;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class IndexWriter implements AutoCloseable {

    private final FileChannel channel;
    private final int indexInterval;
    private int count;

    public IndexWriter(Path path, int indexInterval) throws IOException {
        this.channel = FileChannel.open(path,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND,
                StandardOpenOption.CREATE);
        this.indexInterval = indexInterval;
        this.count = 0;
    }

    public void maybeIndex(long offset, long position) throws IOException {
        // count가 indexInterval의 배수일 때만 기록
        if (count % indexInterval == 0) {
            // [offset: 8바이트][position: 8바이트] 파일에 쓰기
            ByteBuffer buffer = ByteBuffer.allocate(16);
            buffer.putLong(offset);
            buffer.putLong(position);
            buffer.flip();
            channel.write(buffer);
        }
        count++;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}