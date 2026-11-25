package com.hostelswap.controller;

import com.hostelswap.model.Item;
import com.hostelswap.model.User;
import com.hostelswap.repository.ItemRepository;
import com.hostelswap.repository.RatingRepository;
import com.hostelswap.repository.RequestRepository;
import com.hostelswap.repository.SaleRepository;
import com.hostelswap.service.TradeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
public class TradeController {
    private final TradeService trade;
    private final ItemRepository items;
    private final RequestRepository requests;
    private final SaleRepository sales;
    private final RatingRepository ratings;

    public TradeController(TradeService trade, ItemRepository items, RequestRepository requests,
                           SaleRepository sales, RatingRepository ratings) {
        this.trade = trade; this.items = items; this.requests = requests; this.sales = sales; this.ratings = ratings;
    }

    @PostMapping("/send-request")
    public String send(@ModelAttribute(name = "me", binding = false) User me, @RequestParam Long itemId, @RequestParam(required = false) BigDecimal offerPrice, RedirectAttributes ra) {
        try {
            if (offerPrice == null || offerPrice.signum() <= 0) throw new IllegalStateException("Enter a valid offer above 0.");
            Item it = items.findById(itemId).orElse(null);
            trade.sendRequest(it, me, offerPrice);
            ra.addFlashAttribute("flash", "Request sent to the seller.");
            return "redirect:/requests";
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashErr", ex.getMessage());
            return "redirect:/item/" + itemId;
        }
    }

    @GetMapping("/requests")
    public String requestList(@ModelAttribute(name = "me", binding = false) User me, Model m) {
        m.addAttribute("myRequests", requests.findForUser(me.getId()));
        return "requests";
    }

    @PostMapping("/respond-request")
    public String respond(@ModelAttribute(name = "me", binding = false) User me, @RequestParam Long id, @RequestParam String action,
                          @RequestParam(required = false) BigDecimal counterPrice, RedirectAttributes ra) {
        try {
            String msg = trade.respond(id, me, action, counterPrice);
            ra.addFlashAttribute("flash", msg);
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashErr", ex.getMessage());
        }
        return "redirect:/requests";
    }

    @PostMapping("/sale-action")
    public String saleAction(@ModelAttribute(name = "me", binding = false) User me, @RequestParam Long id, @RequestParam String action, RedirectAttributes ra) {
        boolean complete = "complete".equals(action);
        try {
            String msg = trade.finish(id, me, complete);
            ra.addFlashAttribute("flash", msg);
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashErr", ex.getMessage());
        }
        return complete ? "redirect:/history" : "redirect:/requests";
    }

    @GetMapping("/history")
    public String history(@ModelAttribute(name = "me", binding = false) User me, Model m) {
        m.addAttribute("sales", sales.findForUser(me.getId()));
        m.addAttribute("ratedIds", ratings.ratedSaleIds(me.getId()));
        return "history";
    }

    @PostMapping("/rate")
    public String rate(@ModelAttribute(name = "me", binding = false) User me, @RequestParam Long saleId, @RequestParam int score,
                       @RequestParam(required = false) String comment, RedirectAttributes ra) {
        try {
            trade.rate(saleId, me, score, comment);
            ra.addFlashAttribute("flash", "Thanks for rating.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashErr", ex.getMessage());
        }
        return "redirect:/history";
    }
}
