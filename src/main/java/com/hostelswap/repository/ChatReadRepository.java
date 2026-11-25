package com.hostelswap.repository;

import com.hostelswap.model.ChatRead;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatReadRepository extends JpaRepository<ChatRead, Long> {
    Optional<ChatRead> findByRequestIdAndUserId(Long requestId, Long userId);
}
