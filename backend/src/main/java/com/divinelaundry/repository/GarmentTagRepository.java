package com.divinelaundry.repository;

import com.divinelaundry.domain.GarmentTag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GarmentTagRepository extends JpaRepository<GarmentTag, Long> {
    @EntityGraph(attributePaths = {"order", "orderItem"})
    List<GarmentTag> findByOrder_IdOrderByOrderItem_IdAscPieceSequenceAsc(Long orderId);
}
