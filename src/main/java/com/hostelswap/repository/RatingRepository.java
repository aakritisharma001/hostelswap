package com.hostelswap.repository;

import com.hostelswap.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    boolean existsBySaleIdAndRaterId(Long saleId, Long raterId);
    List<Rating> findByRatedIdOrderByIdDesc(Long ratedId);

    @Query("select r.sale.id from Rating r where r.rater.id = :u")
    List<Long> ratedSaleIds(@Param("u") Long raterId);

    @Query("select avg(r.score) from Rating r where r.rated.id = :u")
    Double averageFor(@Param("u") Long ratedId);

    @Query("select avg(r.score) from Rating r")
    Double averageAll();
}
