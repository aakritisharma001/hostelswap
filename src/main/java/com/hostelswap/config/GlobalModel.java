package com.hostelswap.config;

import com.hostelswap.model.User;
import com.hostelswap.repository.RequestRepository;
import com.hostelswap.repository.UserRepository;
import com.hostelswap.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Values available on every page: the logged-in user, the photo limit, and the navbar badge count.
@ControllerAdvice
public class GlobalModel {
    private final UserRepository users;
    private final RequestRepository requests;
    private final ChatService chat;

    @Value("${hostelswap.max-photo-mb}")
    private int maxMb;

    public GlobalModel(UserRepository users, RequestRepository requests, ChatService chat) {
        this.users = users;
        this.requests = requests;
        this.chat = chat;
    }

    @ModelAttribute("maxPhotoMb")
    public int maxPhotoMb() { return maxMb; }

    @ModelAttribute("me")
    public User me(HttpSession session) {
        if (session == null) return null;
        Object id = session.getAttribute("uid");
        return id == null ? null : users.findById((Long) id).orElse(null);
    }

    // Deals waiting for this user's reply, plus unread chat messages
    @ModelAttribute("pendingCount")
    public long pendingCount(HttpSession session) {
        if (session == null) return 0;
        Object id = session.getAttribute("uid");
        if (id == null) return 0;
        Long uid = (Long) id;
        return requests.countActionable(uid) + chat.unreadCount(uid);
    }
}
