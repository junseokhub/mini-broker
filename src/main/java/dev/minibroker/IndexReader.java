package dev.minibroker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class IndexReader implements AutoCloseable {

    private final FileChannel channel;

    public IndexReader(Path path) throws IOException {
        this.channel = FileChannel.open(path, StandardOpenOption.READ);
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