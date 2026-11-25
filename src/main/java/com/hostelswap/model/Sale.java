package com.hostelswap.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// A completed deal (shown in History).
@Entity
@Table(name = "sales")
public class Sale {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Item item;
    @ManyToOne(optional = false) private User buyer;
    @ManyToOne(optional = false) private User seller;
    @Column(precision = 10, scale = 2) private BigDecimal finalPrice;
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Item getItem() { return item; } public void setItem(Item item) { this.item = item; }
    public User getBuyer() { return buyer; } public void setBuyer(User buyer) { this.buyer = buyer; }
    public User getSeller() { return seller; } public void setSeller(User seller) { this.seller = seller; }
    public BigDecimal getFinalPrice() { return finalPrice; } public void setFinalPrice(BigDecimal p) { this.finalPrice = p; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
