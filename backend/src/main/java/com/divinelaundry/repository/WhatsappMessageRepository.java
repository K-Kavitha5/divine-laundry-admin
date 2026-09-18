package com.divinelaundry.repository;

import com.divinelaundry.domain.WhatsappMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WhatsappMessageRepository extends JpaRepository<WhatsappMessage, Long> {
    @EntityGraph(attributePaths = {"order", "order.customer"})
    Optional<WhatsappMessage> findByDeduplicationKey(String deduplicationKey);
}
