package com.syntricdb;

import com.syntricdb.engine.StorageEngine;
import com.syntricdb.engine.schema.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StorageEngineTest {

    @TempDir
    Path tempDir;

    private StorageEngine storageEngine;

    @BeforeEach
    public void setup() throws Exception {
        storageEngine = new StorageEngine(tempDir);
        TableSchema schema = new TableSchema("products")
                .addColumn(new ColumnDef("id", ColumnType.VARCHAR, true, true))
                .addColumn(new ColumnDef("title", ColumnType.VARCHAR, false, true))
                .addColumn(new ColumnDef("price", ColumnType.DOUBLE, false, false));
        storageEngine.createTable(schema);
    }

    @Test
    public void testInsertAndGetByPrimaryKey() throws Exception {
        Tuple t = new Tuple();
        t.set("id", "prod_1");
        t.set("title", "High Speed SSD");
        t.set("price", 149.99);

        storageEngine.insert("products", t);

        Tuple fetched = storageEngine.getByPrimaryKey("products", "prod_1");
        assertNotNull(fetched);
        assertEquals("prod_1", fetched.getString("id"));
        assertEquals("High Speed SSD", fetched.getString("title"));
        assertEquals(149.99, fetched.getDouble("price"));
    }

    @Test
    public void testScanAll() throws Exception {
        for (int i = 1; i <= 5; i++) {
            Tuple t = new Tuple();
            t.set("id", "item_" + i);
            t.set("title", "Item #" + i);
            storageEngine.insert("products", t);
        }

        List<Tuple> all = storageEngine.scanAll("products");
        assertEquals(5, all.size());
    }
}
