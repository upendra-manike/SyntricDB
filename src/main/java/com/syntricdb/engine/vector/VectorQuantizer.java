package com.syntricdb.engine.vector;

import java.nio.ByteBuffer;

/**
 * 8-Bit Scalar Quantization (SQ8) utility for compressing 32-bit float vectors into 8-bit byte representations.
 * Reduces vector memory footprint by 75% with minimal similarity precision loss.
 */
public class VectorQuantizer {

    public static class QuantizedVector {
        private final byte[] data;
        private final float minVal;
        private final float scale;
        private final int dimension;

        public QuantizedVector(byte[] data, float minVal, float scale, int dimension) {
            this.data = data;
            this.minVal = minVal;
            this.scale = scale;
            this.dimension = dimension;
        }

        public byte[] getData() { return data; }
        public float getMinVal() { return minVal; }
        public float getScale() { return scale; }
        public int getDimension() { return dimension; }

        public float[] dequantize() {
            float[] result = new float[dimension];
            for (int i = 0; i < dimension; i++) {
                int unsignedByte = data[i] & 0xFF;
                result[i] = minVal + (unsignedByte * scale);
            }
            return result;
        }

        public byte[] serialize() {
            ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 4 + data.length);
            buffer.putInt(dimension);
            buffer.putFloat(minVal);
            buffer.putFloat(scale);
            buffer.put(data);
            return buffer.array();
        }

        public static QuantizedVector deserialize(byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            int dim = buffer.getInt();
            float minVal = buffer.getFloat();
            float scale = buffer.getFloat();
            byte[] data = new byte[dim];
            buffer.get(data);
            return new QuantizedVector(data, minVal, scale, dim);
        }
    }

    public static QuantizedVector quantize(float[] vector) {
        if (vector == null || vector.length == 0) {
            return new QuantizedVector(new byte[0], 0f, 0f, 0);
        }

        float min = vector[0];
        float max = vector[0];
        for (float v : vector) {
            if (v < min) min = v;
            if (v > max) max = v;
        }

        float range = max - min;
        float scale = range > 1e-7f ? range / 255.0f : 1.0f;
        byte[] quantized = new byte[vector.length];

        for (int i = 0; i < vector.length; i++) {
            int val = Math.round((vector[i] - min) / scale);
            val = Math.max(0, Math.min(255, val));
            quantized[i] = (byte) val;
        }

        return new QuantizedVector(quantized, min, scale, vector.length);
    }

    public static float estimatedCosineDistance(QuantizedVector q1, QuantizedVector q2) {
        float[] v1 = q1.dequantize();
        float[] v2 = q2.dequantize();
        return VectorSIMD.cosineDistance(v1, v2);
    }
}
