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
        List<GarmentTag> created = new ArrayList<>();
        int itemPosition = 0;
        for (OrderItem item : order.getItems()) {
            itemPosition++;
            if (item.isNoPrint()) continue;
            for (int sequence = 1; sequence <= item.getPieceCount(); sequence++) {
                if (hasTag(existing, item, sequence)) continue;
                String tagNumber = "%s-%06d-%02d".formatted(
                        order.getOrderNumber().replace("SO-", "TAG-"), itemReference(item, itemPosition), sequence);
                created.add(new GarmentTag(tagNumber, order, item, sequence));
            }
        }
        if (created.isEmpty()) return existing;
        List<GarmentTag> saved = tags.saveAll(created);
        List<GarmentTag> complete = new ArrayList<>(existing);
        complete.addAll(saved);
        return complete;
    }

    @Transactional
    public void markPrinted(LaundryOrder order) {
        List<GarmentTag> ensured = ensureTags(order);
        ensured.forEach(GarmentTag::markPrinted);
        tags.saveAll(ensured);
    }

    private boolean hasTag(List<GarmentTag> existing, OrderItem item, int sequence) {
        return existing.stream().anyMatch(tag -> tag.getPieceSequence() == sequence
                && (tag.getOrderItem() == item
                || (tag.getOrderItem() != null && item.getId() != null
                && item.getId().equals(tag.getOrderItem().getId()))));
    }

    private Object itemReference(OrderItem item, int itemPosition) {
        return item.getId() == null ? itemPosition : item.getId();
    }
}
