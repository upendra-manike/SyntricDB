package com.example.syntricdb.openapi.repository;

import com.example.syntricdb.openapi.entity.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, String> {

    // 1. Standard JPA Derived Query
    List<ItemEntity> findByCategory(String category);

    // 2. Native SyntricDB HNSW Vector Similarity Search
    @Query(value = "SELECT * FROM items WHERE category = :category AND embedding SIMILAR TO :term TOP :limit", nativeQuery = true)
    List<ItemEntity> searchSimilarItems(@Param("category") String category,
                                        @Param("term") String term,
                                        @Param("limit") int limit);

    // 3. Native SyntricDB In-Engine AI RAG Query
    @Query(value = "SELECT AI_RAG(:prompt)", nativeQuery = true)
    String generateAIRagResponse(@Param("prompt") String prompt);
}
