package com.example.syntricdb.openapi;

import com.example.syntricdb.openapi.entity.ItemEntity;
import com.example.syntricdb.openapi.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SyntricDbOpenApiTest {

    @Autowired
    private ItemService itemService;

    @Test
    public void testSyntricDbSpringDataJpaAndOpenApiIntegration() {
        System.out.println("=================================================");
        System.out.println("🚀 Testing Spring Boot 3 + OpenAPI + SyntricDB Integration");
        System.out.println("=================================================");

        // 1. Create and Save Item via JPA
        ItemEntity item = new ItemEntity(
                "item_201",
                "MacBook Pro M3 Max",
                "Electronics",
                "16-core CPU, 40-core GPU, 128GB Unified Memory for AI Database development",
                3499.99
        );
        ItemEntity saved = itemService.saveItem(item);
        assertNotNull(saved);
        assertEquals("item_201", saved.getId());
        System.out.println("✅ Saved Item via JPA: " + saved.getName());

        // 2. Fetch Item by ID
        Optional<ItemEntity> fetched = itemService.getItemById("item_201");
        assertTrue(fetched.isPresent());
        assertEquals("Electronics", fetched.get().getCategory());
        System.out.println("✅ Fetched Item by ID: " + fetched.get().getName() + " | $" + fetched.get().getPrice());

        // 3. Perform HNSW Vector Similarity Search
        List<ItemEntity> vectorResults = itemService.searchSimilarItems("Electronics", "developer laptop", 5);
        assertNotNull(vectorResults);
        System.out.println("🔍 SyntricDB HNSW Vector Search Results Count: " + vectorResults.size());

        // 4. Perform In-Engine AI RAG Query
        String ragAnswer = itemService.generateAIRagResponse("What is the top recommended laptop for software engineers?");
        assertNotNull(ragAnswer);
        assertTrue(ragAnswer.contains("SyntricDB"));
        System.out.println("🤖 SyntricDB In-Engine AI RAG Answer:\n   " + ragAnswer);

        System.out.println("=================================================");
        System.out.println("✅ ALL SPRING BOOT 3 + OPENAPI + SYNTRICDB TESTS PASSED!");
        System.out.println("=================================================");
    }
}
