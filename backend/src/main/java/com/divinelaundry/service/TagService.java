package com.divinelaundry.service;

import com.divinelaundry.domain.GarmentTag;
import com.divinelaundry.domain.LaundryOrder;
import com.divinelaundry.domain.OrderItem;
import com.divinelaundry.repository.GarmentTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class TagService {
    private final GarmentTagRepository tags;

    public TagService(GarmentTagRepository tags) {
        this.tags = tags;
    }

    @Transactional
    public List<GarmentTag> ensureTags(LaundryOrder order) {
        List<GarmentTag> existing = tags.findByOrder_IdOrderByOrderItem_IdAscPieceSequenceAsc(order.getId());
        if (!existing.isEmpty()) return existing;

        List<GarmentTag> created = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            if (item.isNoPrint()) continue;
            for (int sequence = 1; sequence <= item.getPieceCount(); sequence++) {
                String tagNumber = "%s-%06d-%02d".formatted(
                        order.getOrderNumber().replace("SO-", "TAG-"), item.getId(), sequence);
                created.add(new GarmentTag(tagNumber, order, item, sequence));
            }
        }
        return tags.saveAll(created);
    }

    @Transactional
    public void markPrinted(LaundryOrder order) {
        ensureTags(order).forEach(GarmentTag::markPrinted);
    }
}
