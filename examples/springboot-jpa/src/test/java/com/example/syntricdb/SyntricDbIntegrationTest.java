package com.example.syntricdb;

import com.example.syntricdb.entity.Product;
import com.example.syntricdb.service.ProductService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class SyntricDbIntegrationTest {

    @Autowired
    private ProductService productService;

    @Test
    public void testSyntricDbCrudAndAiOperations() {
        System.out.println("Starting SyntricDB Integration Test...");

        // 1. Create & Insert Record
        Product laptop = new Product(
            "test_prod_1",
            "MacBook Pro M3 Max",
            "Electronics",
            3499.99,
            "Ultimate Apple Silicon developer laptop for machine learning and AI databases."
        );
        Product saved = productService.createProduct(laptop);
        Assertions.assertNotNull(saved);
        Assertions.assertEquals("test_prod_1", saved.getId());

        // 2. Vector Similarity Search
        List<Product> matches = productService.searchSimilarProducts("Electronics", "machine learning laptop", 1);
        Assertions.assertNotNull(matches);

        // 3. Native AI RAG Query
        String ragAnswer = productService.askDatabaseRag("What is the best laptop for software developers?");
        Assertions.assertNotNull(ragAnswer);
        Assertions.assertTrue(ragAnswer.contains("SyntricDB RAG Answer"));

        System.out.println("✅ SyntricDB Spring Boot JPA Integration Test PASSED!");
    }
}
