package com.hostelswap.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// One negotiation between a buyer and a seller on one item.
@Entity
@Table(name = "requests")
public class Request {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Item item;
    @ManyToOne(optional = false) private User buyer;
    @ManyToOne(optional = false) private User seller;
    @Column(precision = 10, scale = 2) private BigDecimal offerPrice;
    private Long lastActorId;   // who made the latest offer; the other party must reply
    private String status = "PENDING"; // PENDING / COUNTERED / ACCEPTED / REJECTED / COMPLETED / CANCELLED
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Item getItem() { return item; } public void setItem(Item item) { this.item = item; }
    public User getBuyer() { return buyer; } public void setBuyer(User buyer) { this.buyer = buyer; }
    public User getSeller() { return seller; } public void setSeller(User seller) { this.seller = seller; }
    public BigDecimal getOfferPrice() { return offerPrice; } public void setOfferPrice(BigDecimal p) { this.offerPrice = p; }
    public Long getLastActorId() { return lastActorId; } public void setLastActorId(Long id) { this.lastActorId = id; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
