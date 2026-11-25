package com.hostelswap.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// A student's report about a listing, reviewed by the admin.
@Entity
@Table(name = "reports")
public class Report {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Item item;
    @ManyToOne(optional = false) private User reporter;
    @Column(length = 255) private String reason;
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Item getItem() { return item; } public void setItem(Item item) { this.item = item; }
    public User getReporter() { return reporter; } public void setReporter(User r) { this.reporter = r; }
    public String getReason() { return reason; } public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
