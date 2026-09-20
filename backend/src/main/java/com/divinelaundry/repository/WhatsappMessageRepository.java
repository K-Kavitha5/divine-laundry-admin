package com.divinelaundry.repository;

import com.divinelaundry.domain.WhatsappMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.time.Instant;

public interface WhatsappMessageRepository extends JpaRepository<WhatsappMessage, Long> {
    @EntityGraph(attributePaths = {"order", "order.customer"})
    Optional<WhatsappMessage> findByDeduplicationKey(String deduplicationKey);

        @Modifying
        @Query(value = """
            UPDATE whatsapp_messages
               SET delivery_status = 'PENDING',
               claimed_at = :claimedAt,
               attempt_count = attempt_count + 1,
               last_error = NULL
             WHERE deduplication_key = :deduplicationKey
               AND delivery_status NOT IN ('SENT', 'DELIVERED')
               AND (delivery_status <> 'PENDING'
                OR claimed_at IS NULL
                OR claimed_at <= :staleBefore)
            """, nativeQuery = true)
        int claimForDelivery(
            @Param("deduplicationKey") String deduplicationKey,
            @Param("claimedAt") Instant claimedAt,
            @Param("staleBefore") Instant staleBefore);
}
