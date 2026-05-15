package dev.minibroker.log.segment;

import dev.minibroker.log.record.Record;
import dev.minibroker.log.record.RecordWriter;
import dev.minibroker.log.index.IndexWriter;

import java.io.IOException;
import java.nio.file.Path;

public class LogSegment implements AutoCloseable {

    private final RecordWriter recordWriter;
    private final IndexWriter indexWriter;

    public LogSegment(Path logPath, Path indexPath, int indexInterval) throws IOException {
        this.recordWriter = new RecordWriter(logPath);
        this.indexWriter = new IndexWriter(indexPath, indexInterval);
    }

    public void append(Record record) throws IOException {
        long position = recordWriter.append(record);
        indexWriter.maybeIndex(record.offset(), position);
    }

    @Override
    public void close() throws IOException {
        recordWriter.close();
        indexWriter.close();
    }
}
