package com.hostelswap.controller;

import com.hostelswap.model.Message;
import com.hostelswap.model.Request;
import com.hostelswap.model.User;
import com.hostelswap.repository.MessageRepository;
import com.hostelswap.repository.RequestRepository;
import com.hostelswap.service.ChatService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.regex.Pattern;

// Deal chat between buyer and seller. Only available after an offer is accepted.
@Controller
public class ChatController {
    private final RequestRepository requests;
    private final MessageRepository messages;
    private final ChatService chat;

    private static final Pattern PHONE = Pattern.compile("^\\+?\\d{10,13}$");
    private static final Pattern LONG_NUMBER = Pattern.compile(".*\\d{6,}.*");

    public ChatController(RequestRepository requests, MessageRepository messages, ChatService chat) {
        this.requests = requests;
        this.messages = messages;
        this.chat = chat;
    }

    @GetMapping("/chat/{id}")
    public String chat(@PathVariable Long id, @ModelAttribute(name = "me", binding = false) User me,
                       Model m, RedirectAttributes ra) {
        Request r = requests.findById(id).orElse(null);
        if (r == null || !isParty(r, me)) {
            ra.addFlashAttribute("flashErr", "Chat not found.");
            return "redirect:/requests";
        }
        m.addAttribute("req", r);
        m.addAttribute("messages", messages.findByRequestIdOrderByIdAsc(id));
        m.addAttribute("chatOpen", "ACCEPTED".equals(r.getStatus()));
        chat.markRead(id, me.getId());
        return "chat";
    }

    @PostMapping("/chat/{id}/send")
    public String send(@PathVariable Long id, @ModelAttribute(name = "me", binding = false) User me,
                       @RequestParam String type,
                       @RequestParam(required = false) String text,
                       @RequestParam(required = false) String place,
                       @RequestParam(required = false) String time,
                       @RequestParam(required = false) String phone,
                       RedirectAttributes ra) {
        Request r = requests.findById(id).orElse(null);
        if (r == null || !isParty(r, me)) {
            ra.addFlashAttribute("flashErr", "Chat not found.");
            return "redirect:/requests";
        }
        if (!"ACCEPTED".equals(r.getStatus())) {
            ra.addFlashAttribute("flashErr", "This chat is closed. The deal is no longer active.");
            return "redirect:/chat/" + id;
        }

        String msgType;
        String body;
        try {
            switch (type) {
                case "meeting" -> {
                    need(place, "Enter a meeting place.");
                    need(time, "Choose a meeting time.");
                    msgType = "MEETING";
                    body = "Meeting place: " + place.trim() + " | Time: " + time.replace("T", " ");
                }
                case "phone" -> {
                    String p = phone == null ? "" : phone.replace(" ", "").trim();
                    if (!PHONE.matcher(p).matches())
                        throw new IllegalStateException("Enter a valid phone number (10 digits, optionally starting with +).");
                    msgType = "PHONE";
                    body = p;
                }
                case "phone-decline" -> {
                    msgType = "PHONE_DECLINED";
                    body = "Prefers not to share a phone number. Please continue here in chat.";
                }
                case "text" -> {
                    String t = text == null ? "" : text.trim();
                    need(t, "Type a message first.");
                    if (t.length() > 500) throw new IllegalStateException("Message is too long (max 500 characters).");
                    if (LONG_NUMBER.matcher(t).matches())
                        throw new IllegalStateException("Please don't type phone numbers in chat. Use the Share phone number option.");
                    msgType = "TEXT";
                    body = t;
                }
                default -> throw new IllegalStateException("Unknown message type.");
            }
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashErr", ex.getMessage());
            return "redirect:/chat/" + id;
        }

        Message msg = new Message();
        msg.setRequest(r);
        msg.setSender(me);
        msg.setType(msgType);
        msg.setBody(body);
        messages.save(msg);
        return "redirect:/chat/" + id;
    }

    private boolean isParty(Request r, User me) {
        return r.getBuyer().getId().equals(me.getId()) || r.getSeller().getId().equals(me.getId());
    }

    private void need(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalStateException(message);
    }
}
