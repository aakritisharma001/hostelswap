package com.hostelswap.service;

import com.hostelswap.model.Item;
import com.hostelswap.model.Rating;
import com.hostelswap.model.Request;
import com.hostelswap.model.Sale;
import com.hostelswap.model.User;
import com.hostelswap.repository.ItemRepository;
import com.hostelswap.repository.RatingRepository;
import com.hostelswap.repository.RequestRepository;
import com.hostelswap.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {
    @Mock RequestRepository requests;
    @Mock ItemRepository items;
    @Mock SaleRepository sales;
    @Mock RatingRepository ratings;

    private TradeService service;

    @BeforeEach
    void setUp() {
        service = new TradeService(requests, items, sales, ratings);
    }

    private User user(long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private Item item(long id, User seller, String status) {
        Item it = new Item();
        it.setId(id);
        it.setSeller(seller);
        it.setPrice(BigDecimal.TEN);
        it.setStatus(status);
        return it;
    }

    private Request request(long id, User buyer, User seller, Item item, Long lastActor, String status) {
        Request r = new Request();
        r.setId(id);
        r.setBuyer(buyer);
        r.setSeller(seller);
        r.setItem(item);
        r.setLastActorId(lastActor);
        r.setStatus(status);
        r.setOfferPrice(BigDecimal.valueOf(500));
        return r;
    }

    // ---------- Sending a request ----------

    @Test
    void sellerCannotRequestOwnItem() {
        User seller = user(1);
        Item it = item(10, seller, "AVAILABLE");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.sendRequest(it, seller, BigDecimal.ONE));
        assertEquals("You cannot request your own item.", ex.getMessage());
    }

    @Test
    void duplicateOpenRequestIsRejected() {
        User seller = user(1), buyer = user(2);
        Item it = item(10, seller, "AVAILABLE");
        when(requests.findByItemIdAndBuyerIdAndStatusIn(eq(10L), eq(2L), anyList()))
                .thenReturn(List.of(new Request()));

        assertThrows(IllegalStateException.class, () -> service.sendRequest(it, buyer, BigDecimal.ONE));
        verify(requests, never()).save(any(Request.class));
    }

    @Test
    void validOfferIsSaved() {
        User seller = user(1), buyer = user(2);
        Item it = item(10, seller, "AVAILABLE");
        when(requests.findByItemIdAndBuyerIdAndStatusIn(eq(10L), eq(2L), anyList())).thenReturn(List.of());

        service.sendRequest(it, buyer, BigDecimal.ONE);
        verify(requests).save(any(Request.class));
    }

    // ---------- Accept, reject, counter ----------

    @Test
    void acceptReservesItemAndRejectsOtherOffers() {
        User seller = user(1), buyer = user(2), other = user(3);
        Item it = item(10, seller, "AVAILABLE");
        Request r = request(100, buyer, seller, it, 2L, "PENDING");
        Request rival = request(101, other, seller, it, 3L, "PENDING");
        when(requests.findById(100L)).thenReturn(Optional.of(r));
        when(requests.findByItemIdAndStatusIn(eq(10L), anyList())).thenReturn(List.of(r, rival));

        String msg = service.respond(100L, seller, "accept", null);

        assertEquals("Accepted. The item is reserved for this deal.", msg);
        assertEquals("RESERVED", it.getStatus());
        assertEquals("ACCEPTED", r.getStatus());
        assertEquals("REJECTED", rival.getStatus());
    }

    @Test
    void acceptFailsWhenItemIsAlreadyReserved() {
        User seller = user(1), buyer = user(2);
        Item it = item(10, seller, "RESERVED");
        Request r = request(100, buyer, seller, it, 2L, "PENDING");
        when(requests.findById(100L)).thenReturn(Optional.of(r));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.respond(100L, seller, "accept", null));
        assertEquals("This item is no longer available.", ex.getMessage());
    }

    @Test
    void personWhoMadeTheLastOfferCannotReply() {
        User seller = user(1), buyer = user(2);
        Item it = item(10, seller, "AVAILABLE");
        Request r = request(100, buyer, seller, it, 1L, "PENDING");
        when(requests.findById(100L)).thenReturn(Optional.of(r));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.respond(100L, seller, "reject", null));
        assertEquals("Waiting for the other person to reply.", ex.getMessage());
    }

    @Test
    void counterOfferUpdatesPriceAndTurn() {
        User seller = user(1), buyer = user(2);
        Item it = item(10, seller, "AVAILABLE");
        Request r = request(100, buyer, seller, it, 1L, "PENDING");
        when(requests.findById(100L)).thenReturn(Optional.of(r));

        service.respond(100L, buyer, "counter", BigDecimal.valueOf(450));

        assertEquals("COUNTERED", r.getStatus());
        assertEquals(BigDecimal.valueOf(450), r.getOfferPrice());
        assertEquals(2L, r.getLastActorId());
    }

    @Test
    void counterWithoutPriceIsRejected() {
        User seller = user(1), buyer = user(2);
        Item it = item(10, seller, "AVAILABLE");
        Request r = request(100, buyer, seller, it, 1L, "PENDING");
        when(requests.findById(100L)).thenReturn(Optional.of(r));

        assertThrows(IllegalStateException.class, () -> service.respond(100L, buyer, "counter", null));
    }

    // ---------- Completing and cancelling ----------

    @Test
    void buyerCannotMarkItemSold() {
        User seller = user(1), buyer = user(2);
        Item it = item(10, seller, "RESERVED");
        Request r = request(100, buyer, seller, it, 1L, "ACCEPTED");
        when(requests.findById(100L)).thenReturn(Optional.of(r));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.finish(100L, buyer, true));
        assertEquals("Only the seller can mark the item as sold.", ex.getMessage());
    }

    @Test
    void sellerCompletesSaleAndMarksItemSold() {
        User seller = user(1), buyer = user(2);
        Item it = item(10, seller, "RESERVED");
        Request r = request(100, buyer, seller, it, 1L, "ACCEPTED");
        when(requests.findById(100L)).thenReturn(Optional.of(r));

        service.finish(100L, seller, true);

        assertEquals("SOLD", it.getStatus());
        assertEquals("COMPLETED", r.getStatus());
        verify(sales).save(any(Sale.class));
    }

    @Test
    void cancellingReleasesItemBackToShop() {
        User seller = user(1), buyer = user(2);
        Item it = item(10, seller, "RESERVED");
        Request r = request(100, buyer, seller, it, 1L, "ACCEPTED");
        when(requests.findById(100L)).thenReturn(Optional.of(r));

        service.finish(100L, buyer, false);

        assertEquals("AVAILABLE", it.getStatus());
        assertEquals("CANCELLED", r.getStatus());
    }

    // ---------- Ratings ----------

    @Test
    void ratingOutsideOneToFiveIsRejected() {
        User seller = user(1), buyer = user(2);
        Sale s = new Sale();
        s.setBuyer(buyer);
        s.setSeller(seller);
        when(sales.findById(5L)).thenReturn(Optional.of(s));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.rate(5L, buyer, 6, "great"));
        assertEquals("Choose a rating from 1 to 5.", ex.getMessage());
    }

    @Test
    void sameDealCannotBeRatedTwice() {
        User seller = user(1), buyer = user(2);
        Sale s = new Sale();
        s.setBuyer(buyer);
        s.setSeller(seller);
        when(sales.findById(5L)).thenReturn(Optional.of(s));
        when(ratings.existsBySaleIdAndRaterId(eq(5L), eq(2L))).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.rate(5L, buyer, 5, "great"));
        verify(ratings, never()).save(any(Rating.class));
    }

    @Test
    void validRatingIsSaved() {
        User seller = user(1), buyer = user(2);
        Sale s = new Sale();
        s.setBuyer(buyer);
        s.setSeller(seller);
        when(sales.findById(5L)).thenReturn(Optional.of(s));

        service.rate(5L, buyer, 4, "smooth deal");
        verify(ratings).save(any(Rating.class));
    }
}
