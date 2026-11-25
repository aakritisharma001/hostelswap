package com.hostelswap.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ratings")
public class Rating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Sale sale;
    @ManyToOne(optional = false) private User rater;
    @ManyToOne(optional = false) private User rated;
    private int score;
    @Column(length = 255) private String comment;
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Sale getSale() { return sale; } public void setSale(Sale sale) { this.sale = sale; }
    public User getRater() { return rater; } public void setRater(User rater) { this.rater = rater; }
    public User getRated() { return rated; } public void setRated(User rated) { this.rated = rated; }
    public int getScore() { return score; } public void setScore(int score) { this.score = score; }
    public String getComment() { return comment; } public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
