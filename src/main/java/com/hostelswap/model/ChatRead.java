package com.hostelswap.model;

import jakarta.persistence.*;

// Remembers the last chat message a user has seen, so the navbar can count unread messages.
@Entity
@Table(name = "chat_reads", uniqueConstraints = @UniqueConstraint(columnNames = {"request_id", "user_id"}))
public class ChatRead {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Request request;
    @ManyToOne(optional = false) private User user;
    private long lastSeenId;

    public Long getId() { return id; }
    public Request getRequest() { return request; } public void setRequest(Request r) { this.request = r; }
    public User getUser() { return user; } public void setUser(User u) { this.user = u; }
    public long getLastSeenId() { return lastSeenId; } public void setLastSeenId(long v) { this.lastSeenId = v; }
}
