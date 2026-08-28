package com.example.syntricdb.openapi.service;

import com.example.syntricdb.openapi.entity.ItemEntity;
import com.example.syntricdb.openapi.repository.ItemRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ItemService {

    @Autowired
    private ItemRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initSchema() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS items (id VARCHAR PRIMARY KEY, name VARCHAR, category VARCHAR, description VARCHAR, price DOUBLE, embedding FLOAT_VECTOR(128))");
            System.out.println("✅ SyntricDB Table 'items' initialized successfully.");
        } catch (Exception e) {
            System.out.println("⚠️ Table init note: " + e.getMessage());
        }
    }

    @Transactional
    public ItemEntity saveItem(ItemEntity item) {
        return repository.save(item);
    }

    public Optional<ItemEntity> getItemById(String id) {
        return repository.findById(id);
    }

    public List<ItemEntity> getAllItems() {
        return repository.findAll();
    }

    public List<ItemEntity> getItemsByCategory(String category) {
        return repository.findByCategory(category);
    }

    public void deleteItem(String id) {
        repository.deleteById(id);
    }

    public List<ItemEntity> searchSimilarItems(String category, String query, int limit) {
        return repository.searchSimilarItems(category, query, limit);
    }

    public String generateAIRagResponse(String prompt) {
        return repository.generateAIRagResponse(prompt);
    }
}
