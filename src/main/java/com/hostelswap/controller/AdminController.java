package com.hostelswap.controller;

import com.hostelswap.model.Item;
import com.hostelswap.model.User;
import com.hostelswap.repository.ItemRepository;
import com.hostelswap.repository.RatingRepository;
import com.hostelswap.repository.ReportRepository;
import com.hostelswap.repository.SaleRepository;
import com.hostelswap.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {
    private final UserRepository users;
    private final ItemRepository items;
    private final SaleRepository sales;
    private final ReportRepository reports;
    private final RatingRepository ratings;

    public AdminController(UserRepository users, ItemRepository items, SaleRepository sales,
                           ReportRepository reports, RatingRepository ratings) {
        this.users = users; this.items = items; this.sales = sales; this.reports = reports; this.ratings = ratings;
    }

    @GetMapping("/admin")
    public String admin(@ModelAttribute(name = "me", binding = false) User me, Model m, RedirectAttributes ra) {
        if (!me.isAdmin()) {
            ra.addFlashAttribute("flashErr", "Admins only.");
            return "redirect:/dashboard";
        }
        var activeItems = items.findAll().stream().filter(i -> !i.isDeleted()).toList();
        Double avg = ratings.averageAll();
        m.addAttribute("users", users.findAll());
        m.addAttribute("items", activeItems);
        m.addAttribute("reports", reports.findAllByOrderByIdDesc());
        m.addAttribute("statUsers", users.count());
        m.addAttribute("statListings", activeItems.size());
        m.addAttribute("statDeals", sales.count());
        m.addAttribute("statReports", reports.count());
        m.addAttribute("statAvgRating", avg == null ? "No ratings yet" : String.format("%.1f / 5", avg));
        return "admin";
    }

    // action = block | unblock | delete-item | dismiss-report
    @PostMapping("/admin-action")
    public String action(@ModelAttribute(name = "me", binding = false) User me,
                         @RequestParam String action, @RequestParam Long id, RedirectAttributes ra) {
        if (!me.isAdmin()) {
            ra.addFlashAttribute("flashErr", "Admins only.");
            return "redirect:/dashboard";
        }
        switch (action) {
            case "block", "unblock" -> {
                if (id.equals(me.getId())) {
                    ra.addFlashAttribute("flashErr", "You cannot block yourself.");
                } else {
                    User u = users.findById(id).orElseThrow();
                    if (!u.isAdmin()) {
                        u.setBlocked(action.equals("block"));
                        users.save(u);
                        ra.addFlashAttribute("flash", "User updated.");
                    }
                }
            }
            case "delete-item" -> {
                Item it = items.findById(id).orElseThrow();
                it.setDeleted(true);
                items.save(it);
                ra.addFlashAttribute("flash", "Listing removed.");
            }
            case "dismiss-report" -> {
                reports.deleteById(id);
                ra.addFlashAttribute("flash", "Report dismissed.");
            }
            default -> ra.addFlashAttribute("flashErr", "Unknown action.");
        }
        return "redirect:/admin";
    }
}
