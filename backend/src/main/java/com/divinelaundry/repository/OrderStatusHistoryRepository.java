package com.divinelaundry.repository;

import com.divinelaundry.domain.OrderStatusHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
	@EntityGraph(attributePaths = {"order"})
	List<OrderStatusHistory> findByOrder_OrderNumberOrderByChangedAtAsc(String orderNumber);
}
