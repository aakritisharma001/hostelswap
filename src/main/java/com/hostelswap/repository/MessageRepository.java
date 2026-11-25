package com.hostelswap.repository;

import com.hostelswap.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRequestIdOrderByIdAsc(Long requestId);

    // Messages in a deal from the other person, newer than the last one this user has seen
    @Query("select count(m) from Message m where m.request.id = :r and m.sender.id <> :u and m.id > :last")
    long countUnread(@Param("r") Long requestId, @Param("u") Long userId, @Param("last") long lastSeenId);

    @Query("select max(m.id) from Message m where m.request.id = :r")
    Long maxId(@Param("r") Long requestId);
}
