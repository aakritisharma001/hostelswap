package com.hostelswap.service;

import com.hostelswap.model.ChatRead;
import com.hostelswap.model.Request;
import com.hostelswap.repository.ChatReadRepository;
import com.hostelswap.repository.MessageRepository;
import com.hostelswap.repository.RequestRepository;
import com.hostelswap.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Tracks which chat messages each user has read, for the navbar badge.
@Service
public class ChatService {
    private final RequestRepository requests;
    private final MessageRepository messages;
    private final ChatReadRepository reads;
    private final UserRepository users;

    public ChatService(RequestRepository requests, MessageRepository messages,
                       ChatReadRepository reads, UserRepository users) {
        this.requests = requests; this.messages = messages; this.reads = reads; this.users = users;
    }

    // Unread messages across all active deals of this user
    public long unreadCount(Long userId) {
        long total = 0;
        for (Request r : requests.findForUser(userId)) {
            if (!"ACCEPTED".equals(r.getStatus())) continue;
            total += messages.countUnread(r.getId(), userId, lastSeen(r.getId(), userId));
        }
        return total;
    }

    @Transactional
    public void markRead(Long requestId, Long userId) {
        Long max = messages.maxId(requestId);
        if (max == null) return;
        ChatRead cr = reads.findByRequestIdAndUserId(requestId, userId).orElseGet(() -> {
            ChatRead n = new ChatRead();
            n.setRequest(requests.findById(requestId).orElseThrow());
            n.setUser(users.findById(userId).orElseThrow());
            return n;
        });
        cr.setLastSeenId(max);
        reads.save(cr);
    }

    private long lastSeen(Long requestId, Long userId) {
        return reads.findByRequestIdAndUserId(requestId, userId).map(ChatRead::getLastSeenId).orElse(0L);
    }
}
