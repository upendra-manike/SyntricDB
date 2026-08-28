package com.example.syntricdb.controller;

import com.example.syntricdb.entity.Product;
import com.example.syntricdb.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product created = productService.createProduct(product);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam(defaultValue = "Electronics") String category,
            @RequestParam(defaultValue = "laptop") String query,
            @RequestParam(defaultValue = "5") int limit) {
        List<Product> results = productService.searchSimilarProducts(category, query, limit);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/rag")
    public ResponseEntity<Map<String, String>> askRag(@RequestParam String prompt) {
        String answer = productService.askDatabaseRag(prompt);
        return ResponseEntity.ok(Map.of("prompt", prompt, "answer", answer));
    }
}
