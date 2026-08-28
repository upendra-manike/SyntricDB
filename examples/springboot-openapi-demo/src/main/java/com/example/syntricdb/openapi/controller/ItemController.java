package com.example.syntricdb.openapi.controller;

import com.example.syntricdb.openapi.entity.ItemEntity;
import com.example.syntricdb.openapi.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
@Tag(name = "SyntricDB Items API", description = "RESTful CRUD, HNSW Vector Similarity Search, and Native AI RAG Operations powered by SyntricDB AI-Native Database")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @PostMapping
    @Operation(summary = "Create or Update Item", description = "Inserts a new Item entity into SyntricDB via Spring Data JPA.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item created successfully"),
            @ApiResponse(responseCode = "500", description = "Internal database error")
    })
    public ResponseEntity<ItemEntity> createItem(@RequestBody ItemEntity item) {
        return ResponseEntity.ok(itemService.saveItem(item));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Item by ID", description = "Retrieves an Item by primary key ID via Spring Data JPA.")
    public ResponseEntity<ItemEntity> getItemById(
            @Parameter(description = "Primary Key Item ID", example = "item_101") @PathVariable String id) {
        return itemService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "List All Items", description = "Returns all Item entities stored in SyntricDB.")
    public ResponseEntity<List<ItemEntity>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllItems());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Item by ID", description = "Deletes an Item entity by primary key ID via Spring Data JPA.")
    public ResponseEntity<Map<String, String>> deleteItem(
            @Parameter(description = "Primary Key Item ID", example = "item_101") @PathVariable String id) {
        itemService.deleteItem(id);
        return ResponseEntity.ok(Map.of("message", "Item deleted successfully", "id", id));
    }

    @GetMapping("/search")
    @Operation(summary = "HNSW Vector Similarity Search", description = "Performs SIMD-accelerated HNSW vector similarity search on item embeddings in SyntricDB.")
    public ResponseEntity<List<ItemEntity>> searchSimilarItems(
            @Parameter(description = "Category filter", example = "Electronics") @RequestParam(defaultValue = "Electronics") String category,
            @Parameter(description = "Natural language search query for vector embedding", example = "laptop for developers") @RequestParam(defaultValue = "laptop") String query,
            @Parameter(description = "Top K results", example = "5") @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(itemService.searchSimilarItems(category, query, limit));
    }

    @GetMapping("/rag")
    @Operation(summary = "In-Engine Native AI RAG Execution", description = "Executes SyntricDB native LLM RAG inference (SELECT AI_RAG(?)) directly inside the database engine.")
    public ResponseEntity<Map<String, String>> askRag(
            @Parameter(description = "RAG prompt question", example = "What is the top recommended laptop for software development?") @RequestParam String prompt) {
        String answer = itemService.generateAIRagResponse(prompt);
        return ResponseEntity.ok(Map.of("prompt", prompt, "answer", answer, "engine", "SyntricDB In-Engine AI"));
    }
}
