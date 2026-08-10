package com.syntricdb.engine.vector;

/**
 * Optimized SIMD-style unrolled vector math utilities for high-throughput distance calculations.
 */
public class VectorSIMD {

    public static float dotProduct(float[] a, float[] b) {
        int length = Math.min(a.length, b.length);
        float sum0 = 0.0f, sum1 = 0.0f, sum2 = 0.0f, sum3 = 0.0f;
        int i = 0;
        int unrolledLimit = length - (length % 4);

        for (; i < unrolledLimit; i += 4) {
            sum0 += a[i] * b[i];
            sum1 += a[i + 1] * b[i + 1];
            sum2 += a[i + 2] * b[i + 2];
            sum3 += a[i + 3] * b[i + 3];
        }
        for (; i < length; i++) {
            sum0 += a[i] * b[i];
        }

        return sum0 + sum1 + sum2 + sum3;
    }

    public static float euclideanDistanceSq(float[] a, float[] b) {
        int length = Math.min(a.length, b.length);
        float sum0 = 0.0f, sum1 = 0.0f, sum2 = 0.0f, sum3 = 0.0f;
        int i = 0;
        int unrolledLimit = length - (length % 4);

        for (; i < unrolledLimit; i += 4) {
            float d0 = a[i] - b[i];
            float d1 = a[i + 1] - b[i + 1];
            float d2 = a[i + 2] - b[i + 2];
            float d3 = a[i + 3] - b[i + 3];
            sum0 += d0 * d0;
            sum1 += d1 * d1;
            sum2 += d2 * d2;
            sum3 += d3 * d3;
        }
        for (; i < length; i++) {
            float diff = a[i] - b[i];
            sum0 += diff * diff;
        }

        return sum0 + sum1 + sum2 + sum3;
    }

    public static float cosineDistance(float[] a, float[] b) {
        float dot = dotProduct(a, b);
        float normA = dotProduct(a, a);
        float normB = dotProduct(b, b);

        if (normA == 0.0f || normB == 0.0f) {
            return 1.0f;
        }
        float similarity = dot / (float) (Math.sqrt(normA) * Math.sqrt(normB));
        return 1.0f - Math.max(-1.0f, Math.min(1.0f, similarity));
    }
}
