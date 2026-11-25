package com.hostelswap.repository;

import com.hostelswap.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("select s from Sale s where s.buyer.id = :u or s.seller.id = :u order by s.id desc")
    List<Sale> findForUser(@Param("u") Long userId);

    @Query("select count(s) from Sale s where s.seller.id = :u")
    long countAsSeller(@Param("u") Long userId);
}
