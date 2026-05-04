package dev.minibroker;

import java.util.Arrays;

public class Record {

    private final long offset;
    private final byte[] value;
    private final long timestamp;
    private final byte[] key;

    public Record(long offset, byte[] value, long timestamp, byte[] key) {
        this.offset = offset;
        this.value = value;
        this.timestamp = timestamp;
        this.key = key;
    }
    public long offset() { return offset; }
    public byte[] value() { return value; }
    public long timestamp() { return timestamp; }
    public byte[] key() { return key; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof  Record r)) return false;
        return offset == r.offset
                && timestamp == r.timestamp
                && Arrays.equals(key, r.key)
                && Arrays.equals(value, r.value);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(offset);
        result = 31 * result + Long.hashCode(timestamp);
        result = 31 * result + Arrays.hashCode(key);
        result = 31 * result + Arrays.hashCode(value);
        return result;
    }

    @Override
    public String toString() {
        return "Record{" +
                "offset=" + offset +
                ", value='" + value + '\'' +
                ", timestamp=" + timestamp +
                ", key='" + key + '\'' +
                '}';
    }
}
