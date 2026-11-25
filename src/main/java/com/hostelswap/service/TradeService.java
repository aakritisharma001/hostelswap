package com.hostelswap.service;

import com.hostelswap.model.*;
import com.hostelswap.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

// All buying/selling rules live here. Errors are reported with IllegalStateException.
@Service
public class TradeService {
    private final RequestRepository requests;
    private final ItemRepository items;
    private final SaleRepository sales;
    private final RatingRepository ratings;

    public TradeService(RequestRepository requests, ItemRepository items, SaleRepository sales, RatingRepository ratings) {
        this.requests = requests; this.items = items; this.sales = sales; this.ratings = ratings;
    }

    @Transactional
    public void sendRequest(Item item, User buyer, BigDecimal offer) {
        if (item == null || item.isDeleted() || !"AVAILABLE".equals(item.getStatus()))
            throw new IllegalStateException("This item is not available for requests.");
        if (item.getSeller().getId().equals(buyer.getId()))
            throw new IllegalStateException("You cannot request your own item.");
        List<Request> open = requests.findByItemIdAndBuyerIdAndStatusIn(item.getId(), buyer.getId(),
                List.of("PENDING", "COUNTERED", "ACCEPTED"));
        if (!open.isEmpty()) throw new IllegalStateException("You already have an open request for this item.");

        Request r = new Request();
        r.setItem(item);
        r.setBuyer(buyer);
        r.setSeller(item.getSeller());
        r.setOfferPrice(offer);
        r.setLastActorId(buyer.getId());
        requests.save(r);
    }

    @Transactional
    public String respond(Long requestId, User me, String action, BigDecimal price) {
        Request r = requests.findById(requestId).orElseThrow(() -> new IllegalStateException("Request not found."));
        if (!r.getBuyer().getId().equals(me.getId()) && !r.getSeller().getId().equals(me.getId()))
            throw new IllegalStateException("Request not found.");
        if (!isOpen(r)) throw new IllegalStateException("This request is already closed.");
        if (me.getId().equals(r.getLastActorId()))
            throw new IllegalStateException("Waiting for the other person to reply.");

        switch (action) {
            case "accept" -> {
                Item it = r.getItem();
                if (!"AVAILABLE".equals(it.getStatus()))
                    throw new IllegalStateException("This item is no longer available.");
                it.setStatus("RESERVED");
                r.setStatus("ACCEPTED");
                r.setLastActorId(me.getId());
                for (Request other : requests.findByItemIdAndStatusIn(it.getId(), List.of("PENDING", "COUNTERED"))) {
                    if (!other.getId().equals(r.getId())) other.setStatus("REJECTED");
                }
                return "Accepted. The item is reserved for this deal.";
            }
            case "reject" -> {
                r.setStatus("REJECTED");
                r.setLastActorId(me.getId());
                return "Request rejected.";
            }
            case "counter" -> {
                if (price == null) throw new IllegalStateException("Enter a valid counter-offer above 0.");
                r.setOfferPrice(price);
                r.setStatus("COUNTERED");
                r.setLastActorId(me.getId());
                return "Counter-offer sent.";
            }
            default -> throw new IllegalStateException("Unknown action.");
        }
    }

    @Transactional
    public String finish(Long requestId, User me, boolean complete) {
        Request r = requests.findById(requestId).orElseThrow(() -> new IllegalStateException("Deal not found."));
        if (!r.getBuyer().getId().equals(me.getId()) && !r.getSeller().getId().equals(me.getId()))
            throw new IllegalStateException("Deal not found.");
        if (!"ACCEPTED".equals(r.getStatus())) throw new IllegalStateException("This deal is not active.");

        if (complete) {
            if (!r.getSeller().getId().equals(me.getId()))
                throw new IllegalStateException("Only the seller can mark the item as sold.");
            Item it = r.getItem();
            it.setStatus("SOLD");
            r.setStatus("COMPLETED");
            Sale s = new Sale();
            s.setItem(it);
            s.setBuyer(r.getBuyer());
            s.setSeller(r.getSeller());
            s.setFinalPrice(r.getOfferPrice());
            sales.save(s);
            return "Sale completed. You can now rate the other party.";
        } else {
            r.getItem().setStatus("AVAILABLE");
            r.setStatus("CANCELLED");
            return "Deal cancelled. The item is listed again.";
        }
    }

    @Transactional
    public void rate(Long saleId, User me, int score, String comment) {
        Sale s = sales.findById(saleId).orElseThrow(() -> new IllegalStateException("Deal not found."));
        boolean asBuyer = s.getBuyer().getId().equals(me.getId());
        boolean asSeller = s.getSeller().getId().equals(me.getId());
        if (!asBuyer && !asSeller) throw new IllegalStateException("Deal not found.");
        if (score < 1 || score > 5) throw new IllegalStateException("Choose a rating from 1 to 5.");
        if (ratings.existsBySaleIdAndRaterId(saleId, me.getId()))
            throw new IllegalStateException("You have already rated this deal.");

        Rating r = new Rating();
        r.setSale(s);
        r.setRater(me);
        r.setRated(asBuyer ? s.getSeller() : s.getBuyer());
        r.setScore(score);
        String c = comment == null ? "" : comment.trim();
        r.setComment(c.length() > 255 ? c.substring(0, 255) : c);
        ratings.save(r);
    }

    private boolean isOpen(Request r) {
        return "PENDING".equals(r.getStatus()) || "COUNTERED".equals(r.getStatus());
    }
}
