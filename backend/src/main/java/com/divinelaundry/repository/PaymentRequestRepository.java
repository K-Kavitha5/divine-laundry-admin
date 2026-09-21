package com.divinelaundry.repository;

import com.divinelaundry.domain.PaymentRequest;
import com.divinelaundry.domain.PaymentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {
    Optional<PaymentRequest> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentRequest> findByProviderReference(String providerReference);

    Optional<PaymentRequest> findByProviderPaymentId(String providerPaymentId);

    List<PaymentRequest> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<PaymentRequest> findByIdAndOrder_OrderNumber(Long id, String orderNumber);

    @Query("select pr from PaymentRequest pr where pr.order.id = :orderId and pr.status in :statuses order by pr.createdAt desc")
    List<PaymentRequest> findByOrderIdAndStatusInOrderByCreatedAtDesc(
            @Param("orderId") Long orderId,
            @Param("statuses") Collection<PaymentRequestStatus> statuses);
}
