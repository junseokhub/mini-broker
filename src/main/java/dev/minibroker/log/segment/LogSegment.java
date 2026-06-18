package dev.minibroker.log.segment;

import dev.minibroker.log.index.IndexWriter;
import dev.minibroker.log.record.Record;
import dev.minibroker.log.record.RecordReader;
import dev.minibroker.log.record.RecordWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class LogSegment implements AutoCloseable {

    private final Path logPath;
    private final Path indexPath;
    private final RecordWriter recordWriter;
    private final IndexWriter indexWriter;
    private long nextOffset;

    public LogSegment(Path logPath, Path indexPath, int indexInterval) throws IOException {
        this.logPath      = logPath;
        this.indexPath    = indexPath;
        this.recordWriter = new RecordWriter(logPath);
        this.indexWriter  = new IndexWriter(indexPath, indexInterval);
        this.nextOffset   = recoverNextOffset(logPath);
    }

    private long recoverNextOffset(Path logPath) throws IOException {
        if (!Files.exists(logPath)) return 0;
        try (RecordReader reader = new RecordReader(logPath)) {
            List<Record> records = reader.readAll();
            return records.isEmpty() ? 0 : records.getLast().offset() + 1;
        }
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

    public Path logPath()   { return logPath; }
    public Path indexPath() { return indexPath; }
}
