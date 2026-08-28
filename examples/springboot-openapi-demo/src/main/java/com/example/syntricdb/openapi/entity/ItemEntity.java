package com.example.syntricdb.openapi.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "items")
@Schema(description = "Item Entity managed by Spring Data JPA & SyntricDB Vector Engine")
public class ItemEntity {

    @Id
    @Schema(description = "Unique Item Identifier", example = "item_101")
    private String id;

    @Schema(description = "Item Name", example = "MacBook Pro M3 Max")
    private String name;

    @Schema(description = "Item Category", example = "Electronics")
    private String category;

    @Column(length = 2000)
    @Schema(description = "Detailed Item Description", example = "16-core CPU, 40-core GPU, 128GB Unified Memory for AI Database development")
    private String description;

    @Schema(description = "Item Price ($)", example = "3499.99")
    private Double price;

    public ItemEntity() {}

    public ItemEntity(String id, String name, String category, String description, Double price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
