package com.syntricdb;

import com.syntricdb.engine.vector.VectorSIMD;
import com.syntricdb.engine.vector.VectorQuantizer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VectorSIMDAndQuantizerTest {

    @Test
    public void testVectorSIMDDotProduct() {
        float[] a = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
        float[] b = {2.0f, 0.5f, 1.0f, 0.0f, 2.0f};
        float expected = (1*2) + (2*0.5f) + (3*1) + (4*0) + (5*2); // 2 + 1 + 3 + 0 + 10 = 16
        float actual = VectorSIMD.dotProduct(a, b);
        assertEquals(expected, actual, 0.001f);
    }

    @Test
    public void testVectorSIMDCosineDistance() {
        float[] v1 = {1.0f, 0.0f, 0.0f};
        float[] v2 = {1.0f, 0.0f, 0.0f};
        float dist = VectorSIMD.cosineDistance(v1, v2);
        assertEquals(0.0f, dist, 0.001f);

        float[] v3 = {0.0f, 1.0f, 0.0f};
        float orthogonalDist = VectorSIMD.cosineDistance(v1, v3);
        assertEquals(1.0f, orthogonalDist, 0.001f);
    }

    @Test
    public void testVectorQuantizerSQ8() {
        float[] raw = {0.1f, 0.5f, 0.9f, -0.2f, 0.0f, 1.5f};
        VectorQuantizer.QuantizedVector q = VectorQuantizer.quantize(raw);

        assertNotNull(q.getData());
        assertEquals(raw.length, q.getDimension());

        float[] restored = q.dequantize();
        assertEquals(raw.length, restored.length);

        // Check quantization precision within small error bound
        for (int i = 0; i < raw.length; i++) {
            assertEquals(raw[i], restored[i], 0.05f);
        }
    }

    @Test
    public void testQuantizedVectorSerialization() {
        float[] raw = {0.25f, 0.75f, 0.33f};
        VectorQuantizer.QuantizedVector q = VectorQuantizer.quantize(raw);
        byte[] serialized = q.serialize();

        VectorQuantizer.QuantizedVector deserialized = VectorQuantizer.QuantizedVector.deserialize(serialized);
        assertEquals(q.getDimension(), deserialized.getDimension());
        assertEquals(q.getMinVal(), deserialized.getMinVal(), 0.0001f);
        assertEquals(q.getScale(), deserialized.getScale(), 0.0001f);
    }
}
