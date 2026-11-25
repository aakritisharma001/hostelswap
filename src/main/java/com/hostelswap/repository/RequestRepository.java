package com.hostelswap.repository;

import com.hostelswap.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    @Query("select r from Request r where r.buyer.id = :u or r.seller.id = :u order by r.id desc")
    List<Request> findForUser(@Param("u") Long userId);

    List<Request> findByItemIdAndBuyerIdAndStatusIn(Long itemId, Long buyerId, List<String> statuses);

    List<Request> findByItemIdAndStatusIn(Long itemId, List<String> statuses);

    // Requests where it is this user's turn to reply
    @Query("select count(r) from Request r where (r.buyer.id = :u or r.seller.id = :u) " +
           "and r.lastActorId <> :u and r.status in ('PENDING','COUNTERED')")
    long countActionable(@Param("u") Long userId);
}
