package dev.minibroker;

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

    public long findPosition(long targetOffset) throws IOException {
        long result = 0; // 못 찾으면 파일 처음부터 읽기
        channel.position(0); // 항상 처음부터 탐색

        ByteBuffer buffer = ByteBuffer.allocate(16);
        while (channel.read(buffer) == 16) {
            buffer.flip();
            long indexOffset   = buffer.getLong();
            long indexPosition = buffer.getLong();
            buffer.clear();

            if (indexOffset <= targetOffset) {
                result = indexPosition;
            } else {
                // index가 정렬된 순서로 저장되어있으니깐 더 읽을 필요가 없음
                break;
            }
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}