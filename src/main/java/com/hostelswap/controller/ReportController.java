package com.hostelswap.controller;

import com.hostelswap.model.Item;
import com.hostelswap.model.Report;
import com.hostelswap.model.User;
import com.hostelswap.repository.ItemRepository;
import com.hostelswap.repository.ReportRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReportController {
    private final ItemRepository items;
    private final ReportRepository reports;

    public ReportController(ItemRepository items, ReportRepository reports) {
        this.items = items;
        this.reports = reports;
    }

    @PostMapping("/report/{itemId}")
    public String report(@PathVariable Long itemId,
                         @ModelAttribute(name = "me", binding = false) User me,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes ra) {
        Item it = items.findById(itemId).orElse(null);
        if (it == null || it.isDeleted()) {
            ra.addFlashAttribute("flashErr", "Listing not found.");
            return "redirect:/dashboard";
        }
        if (reason == null || reason.isBlank()) {
            ra.addFlashAttribute("flashErr", "Please write a reason for the report.");
            return "redirect:/item/" + itemId;
        }
        String text = reason.trim();
        if (text.length() > 255) text = text.substring(0, 255);

        Report r = new Report();
        r.setItem(it);
        r.setReporter(me);
        r.setReason(text);
        reports.save(r);
        ra.addFlashAttribute("flash", "Thanks. The admin will review this listing.");
        return "redirect:/item/" + itemId;
    }
}
