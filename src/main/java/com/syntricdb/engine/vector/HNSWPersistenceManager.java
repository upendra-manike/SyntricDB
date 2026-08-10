package com.syntricdb.engine.vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Persistence manager responsible for snapshotting and restoring HNSW graph vector index instances.
 */
public class HNSWPersistenceManager {
    private static final Logger log = LoggerFactory.getLogger(HNSWPersistenceManager.class);

    public static void saveIndex(HNSWIndex index, Path targetFile) throws IOException {
        if (index == null || targetFile == null) return;
        Files.createDirectories(targetFile.getParent());

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(targetFile)))) {
            out.writeInt(index.getDimension());
            out.writeUTF(index.getMetric().name());

            Map<String, float[]> vectors = index.getVectors();
            out.writeInt(vectors.size());

            for (Map.Entry<String, float[]> entry : vectors.entrySet()) {
                out.writeUTF(entry.getKey());
                float[] vec = entry.getValue();
                out.writeInt(vec.length);
                for (float v : vec) {
                    out.writeFloat(v);
                }
            }
            log.info("Persisted HNSW Index with {} vectors to {}", vectors.size(), targetFile.getFileName());
        }
    }

    public static HNSWIndex loadIndex(Path sourceFile) throws IOException {
        if (sourceFile == null || !Files.exists(sourceFile)) {
            return null;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(sourceFile)))) {
            int dimension = in.readInt();
            String metricName = in.readUTF();
            DistanceMetric metric = DistanceMetric.valueOf(metricName);

            HNSWIndex index = new HNSWIndex(dimension, metric);
            int count = in.readInt();

            for (int i = 0; i < count; i++) {
                String id = in.readUTF();
                int vecLen = in.readInt();
                float[] vec = new float[vecLen];
                for (int j = 0; j < vecLen; j++) {
                    vec[j] = in.readFloat();
                }
                index.insert(id, vec);
            }

            log.info("Loaded HNSW Index with {} vectors from {}", count, sourceFile.getFileName());
            return index;
        }
    }
}
