package com.divinelaundry.repository;

import com.divinelaundry.domain.OrderStatus;
import com.divinelaundry.domain.Payment;
import com.divinelaundry.domain.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @EntityGraph(attributePaths = {"order", "order.customer"})
    Optional<Payment> findByClientRequestId(String clientRequestId);

    @EntityGraph(attributePaths = {"order"})
    List<Payment> findByOrderOrderNumberOrderByPaidAtDesc(String orderNumber);

        @EntityGraph(attributePaths = {"order"})
        List<Payment> findByOrder_IdInOrderByPaidAtDesc(Collection<Long> orderIds);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.order.id = :orderId")
    BigDecimal sumByOrderId(@Param("orderId") Long orderId);

    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.order.paymentStatus in :paymentStatuses
              and p.order.workStatus not in :excludedStatuses
            """)
    BigDecimal sumForOutstandingOrders(
            @Param("paymentStatuses") Collection<PaymentStatus> paymentStatuses,
            @Param("excludedStatuses") Collection<OrderStatus> excludedStatuses);

    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.paidAt >= :startAt and p.paidAt < :endAt
              and p.order.workStatus <> :excludedStatus
            """)
    BigDecimal sumBetween(
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt,
            @Param("excludedStatus") OrderStatus excludedStatus);
}
