package com.hostelswap.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
public class Item {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String title;
    @Column(length = 1000) private String description;
    @ManyToOne(optional = false) private Category category;
    @Column(precision = 10, scale = 2, nullable = false) private BigDecimal price;
    @Column(name = "item_condition") private String condition;
    private String status = "AVAILABLE"; // AVAILABLE / RESERVED / SOLD
    private String imagePath;
    // The photo itself is kept in the database, so it survives server restarts
    @JdbcTypeCode(SqlTypes.VARBINARY) @Column(length = 6000000)
    private byte[] imageData;
    private String imageType;
    @ManyToOne(optional = false) private User seller;
    private boolean deleted;
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; } public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
    public Category getCategory() { return category; } public void setCategory(Category c) { this.category = c; }
    public BigDecimal getPrice() { return price; } public void setPrice(BigDecimal price) { this.price = price; }
    public String getCondition() { return condition; } public void setCondition(String c) { this.condition = c; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getImagePath() { return imagePath; } public void setImagePath(String p) { this.imagePath = p; }
    public User getSeller() { return seller; } public void setSeller(User seller) { this.seller = seller; }
    public boolean isDeleted() { return deleted; } public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public byte[] getImageData() { return imageData; } public void setImageData(byte[] d) { this.imageData = d; }
    public String getImageType() { return imageType; } public void setImageType(String t) { this.imageType = t; }
}
