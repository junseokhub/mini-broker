package dev.minibroker.log.segment;

import dev.minibroker.log.record.Record;
import dev.minibroker.log.record.RecordWriter;
import dev.minibroker.log.index.IndexWriter;

import java.io.IOException;
import java.nio.file.Path;

public class LogSegment implements AutoCloseable {

    private final RecordWriter recordWriter;
    private final IndexWriter indexWriter;
    private long nextOffset = 0;

    public LogSegment(Path logPath, Path indexPath, int indexInterval) throws IOException {
        this.recordWriter = new RecordWriter(logPath);
        this.indexWriter = new IndexWriter(indexPath, indexInterval);
    }

    public long append(byte[] key, byte[] value) throws IOException {
        long offset = nextOffset++;
        Record record = new Record(offset, System.currentTimeMillis(), key, value);

        long position = recordWriter.append(record);
        indexWriter.maybeIndex(offset, position);
        return offset;
    }

    @Override
    public void close() throws IOException {
        recordWriter.close();
        indexWriter.close();
    }
}
