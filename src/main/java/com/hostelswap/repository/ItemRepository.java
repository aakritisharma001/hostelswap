package com.hostelswap.repository;

import com.hostelswap.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // kw is already a LIKE pattern such as "%book%"; cat = 0 means all categories
    @Query("select i from Item i where i.deleted = false and i.status = 'AVAILABLE' and i.seller.blocked = false " +
           "and lower(i.title) like :kw and (:cat = 0 or i.category.id = :cat) order by i.id desc")
    List<Item> search(@Param("kw") String kw, @Param("cat") long cat);

    List<Item> findBySellerIdAndDeletedFalseOrderByIdDesc(Long sellerId);
}
