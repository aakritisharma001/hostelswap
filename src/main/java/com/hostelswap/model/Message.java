package com.hostelswap.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// One message inside a deal chat (TEXT, MEETING, PHONE or PHONE_DECLINED)
@Entity
@Table(name = "messages")
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) private Request request;
    @ManyToOne(optional = false) private User sender;
    private String type;
    @Column(length = 500) private String body;
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Request getRequest() { return request; } public void setRequest(Request r) { this.request = r; }
    public User getSender() { return sender; } public void setSender(User s) { this.sender = s; }
    public String getType() { return type; } public void setType(String type) { this.type = type; }
    public String getBody() { return body; } public void setBody(String body) { this.body = body; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
