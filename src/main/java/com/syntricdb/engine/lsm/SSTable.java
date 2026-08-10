package com.syntricdb.engine.lsm;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;

public class SSTable {
    private final Path dataPath;
    private final Path indexPath;
    private final BloomFilter bloomFilter;
    private final List<IndexEntry> sparseIndex = new ArrayList<>();
    private final String minKey;
    private final String maxKey;

    public static class IndexEntry {
        public final String key;
        public final long offset;

        public IndexEntry(String key, long offset) {
            this.key = key;
            this.offset = offset;
        }
    }

    private SSTable(Path dataPath, Path indexPath, BloomFilter bloomFilter, List<IndexEntry> sparseIndex, String minKey, String maxKey) {
        this.dataPath = dataPath;
        this.indexPath = indexPath;
        this.bloomFilter = bloomFilter;
        this.sparseIndex.addAll(sparseIndex);
        this.minKey = minKey;
        this.maxKey = maxKey;
    }

    public static SSTable create(Path dir, String sstableId, Map<String, byte[]> sortedData) throws IOException {
        Files.createDirectories(dir);
        Path dataPath = dir.resolve("sstable_" + sstableId + ".db");
        Path indexPath = dir.resolve("sstable_" + sstableId + ".idx");

        BloomFilter bloomFilter = new BloomFilter(Math.max(10, sortedData.size()), 0.01);
        List<IndexEntry> sparseIndex = new ArrayList<>();

        String minKey = null;
        String maxKey = null;

        try (FileChannel dataChannel = FileChannel.open(dataPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileChannel indexChannel = FileChannel.open(indexPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            long offset = 0;
            int count = 0;

            for (Map.Entry<String, byte[]> entry : sortedData.entrySet()) {
                String key = entry.getKey();
                byte[] val = entry.getValue();

                if (minKey == null) minKey = key;
                maxKey = key;

                bloomFilter.add(key);

                // Sparse index: index 1 key every 16 keys
                if (count % 16 == 0) {
                    sparseIndex.add(new IndexEntry(key, offset));

                    byte[] keyBytes = key.getBytes();
                    ByteBuffer idxBuf = ByteBuffer.allocate(4 + keyBytes.length + 8);
                    idxBuf.putInt(keyBytes.length);
                    idxBuf.put(keyBytes);
                    idxBuf.putLong(offset);
                    idxBuf.flip();
                    indexChannel.write(idxBuf);
                }

                byte[] keyBytes = key.getBytes();
                int recordLen = 4 + keyBytes.length + 4 + val.length;
                ByteBuffer dataBuf = ByteBuffer.allocate(recordLen);
                dataBuf.putInt(keyBytes.length);
                dataBuf.put(keyBytes);
                dataBuf.putInt(val.length);
                dataBuf.put(val);
                dataBuf.flip();

                dataChannel.write(dataBuf);
                offset += recordLen;
                count++;
            }
        }

        return new SSTable(dataPath, indexPath, bloomFilter, sparseIndex, minKey, maxKey);
    }

    public byte[] get(String key) throws IOException {
        if (minKey != null && (key.compareTo(minKey) < 0 || key.compareTo(maxKey) > 0)) {
            return null;
        }

        if (!bloomFilter.mightContain(key)) {
            return null;
        }

        // Binary search in sparse index for closest preceding key offset
        long searchOffset = 0;
        int low = 0, high = sparseIndex.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            IndexEntry midVal = sparseIndex.get(mid);
            int cmp = midVal.key.compareTo(key);
            if (cmp <= 0) {
                searchOffset = midVal.offset;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        // Scan from searchOffset in data file
        try (FileChannel channel = FileChannel.open(dataPath, StandardOpenOption.READ)) {
            channel.position(searchOffset);
            ByteBuffer buf = ByteBuffer.allocate(8192);

            while (channel.position() < channel.size()) {
                long pos = channel.position();
                ByteBuffer lenBuf = ByteBuffer.allocate(4);
                if (channel.read(lenBuf) < 4) break;
                lenBuf.flip();
                int kLen = lenBuf.getInt();

                ByteBuffer keyBuf = ByteBuffer.allocate(kLen);
                channel.read(keyBuf);
                String currentKey = new String(keyBuf.array());

                ByteBuffer vLenBuf = ByteBuffer.allocate(4);
                channel.read(vLenBuf);
                vLenBuf.flip();
                int vLen = vLenBuf.getInt();

                byte[] vBytes = new byte[vLen];
                ByteBuffer valBuf = ByteBuffer.wrap(vBytes);
                channel.read(valBuf);

                if (currentKey.equals(key)) {
                    return vBytes;
                }

                if (currentKey.compareTo(key) > 0) {
                    // Passed key
                    break;
                }
            }
        }

        return null;
    }

    public byte[] getMemoryMapped(String key) throws IOException {
        if (!Files.exists(dataPath)) return null;
        try (FileChannel channel = FileChannel.open(dataPath, StandardOpenOption.READ)) {
            long size = channel.size();
            if (size == 0) return null;
            java.nio.MappedByteBuffer mmap = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
            return get(key);
        }
    }

    public String getMinKey() { return minKey; }
    public String getMaxKey() { return maxKey; }
}
