package com.example.syntricdb.service;

import com.example.syntricdb.entity.Product;
import com.example.syntricdb.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initSchema() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS products (id VARCHAR PRIMARY KEY, name VARCHAR, category VARCHAR, price DOUBLE, description VARCHAR, embedding FLOAT_VECTOR(128))");
        } catch (Exception e) {
            System.out.println("Schema init note: " + e.getMessage());
        }
    }

    @Transactional
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> getProductsByCategoryAndMaxPrice(String category, Double maxPrice) {
        return productRepository.findByCategoryAndPriceLessThan(category, maxPrice);
    }

    public List<Product> searchSimilarProducts(String category, String queryTerm, int limit) {
        return productRepository.searchByVectorSimilarity(category, queryTerm, limit);
    }

    public String askDatabaseRag(String userPrompt) {
        return productRepository.generateAIRagResponse(userPrompt);
    }
}
