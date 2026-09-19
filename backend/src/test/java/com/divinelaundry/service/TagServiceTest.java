package com.divinelaundry.service;

import com.divinelaundry.domain.Customer;
import com.divinelaundry.domain.GarmentTag;
import com.divinelaundry.domain.LaundryOrder;
import com.divinelaundry.domain.LaundryServiceItem;
import com.divinelaundry.domain.OrderItem;
import com.divinelaundry.domain.PricingUnit;
import com.divinelaundry.repository.GarmentTagRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TagServiceTest {
    @Test
    void createsOneStableTagPerPhysicalPieceAcrossMultipleItems() {
        GarmentTagRepository repository = mock(GarmentTagRepository.class);
        TagService service = new TagService(repository);
        LaundryOrder order = orderWithItems(1, 3);
        when(repository.findByOrder_IdOrderByOrderItem_IdAscPieceSequenceAsc(order.getId())).thenReturn(List.of());
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<GarmentTag> first = service.ensureTags(order);

        assertThat(first).hasSize(4);
        assertThat(first).extracting(GarmentTag::getPieceSequence).containsExactly(1, 1, 2, 3);
        assertThat(first).extracting(GarmentTag::getTagNumber).doesNotHaveDuplicates();
        verify(repository).saveAll(any());
    }

    @Test
    void repeatedGenerationReturnsExistingTagsWithoutCreatingDuplicates() {
        GarmentTagRepository repository = mock(GarmentTagRepository.class);
        TagService service = new TagService(repository);
        LaundryOrder order = orderWithItems(2);
        List<GarmentTag> existing = existingTags(order, 2);
        when(repository.findByOrder_IdOrderByOrderItem_IdAscPieceSequenceAsc(order.getId())).thenReturn(existing);

        List<GarmentTag> result = service.ensureTags(order);

        assertThat(result).containsExactlyElementsOf(existing);
        verify(repository, never()).saveAll(any());
        assertThat(result.get(0).getTagNumber()).isEqualTo(existing.get(0).getTagNumber());
    }

    @Test
    void partialExistingTagsAreCompletedWithoutChangingExistingIdentity() {
        GarmentTagRepository repository = mock(GarmentTagRepository.class);
        TagService service = new TagService(repository);
        LaundryOrder order = orderWithItems(3);
        List<GarmentTag> existing = new ArrayList<>(existingTags(order, 1));
        when(repository.findByOrder_IdOrderByOrderItem_IdAscPieceSequenceAsc(order.getId())).thenReturn(existing);
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<GarmentTag> result = service.ensureTags(order);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getTagNumber()).isEqualTo(existing.get(0).getTagNumber());
        verify(repository).saveAll(argThat(tags -> ((List<?>) tags).size() == 2));
    }

    @Test
    void printingIncrementsPrintedStateWithoutCreatingTagsAgain() {
        GarmentTagRepository repository = mock(GarmentTagRepository.class);
        TagService service = new TagService(repository);
        LaundryOrder order = orderWithItems(2);
        List<GarmentTag> existing = existingTags(order, 2);
        when(repository.findByOrder_IdOrderByOrderItem_IdAscPieceSequenceAsc(order.getId())).thenReturn(existing);

        service.markPrinted(order);
        service.markPrinted(order);

        assertThat(existing).allSatisfy(tag -> assertThat(tag.getPrintCount()).isEqualTo(2));
        verify(repository, times(2)).saveAll(existing);
    }

    private static LaundryOrder orderWithItems(int... pieces) {
        Customer customer = new Customer("Test Customer", "9876543210", null, "Trichy");
        LaundryOrder order = new LaundryOrder("tag-request-" + System.nanoTime(), customer, null, null, "admin");
        order.assignOrderNumber("SO-2026-000001");
        for (int index = 0; index < pieces.length; index++) {
            LaundryServiceItem service = new LaundryServiceItem(
                    "SERVICE-" + index, "Service " + index, "Laundry", PricingUnit.PIECE, BigDecimal.TEN);
            order.addItem(new OrderItem(service, BigDecimal.valueOf(pieces[index]), pieces[index], false));
        }
        return order;
    }

    private static List<GarmentTag> existingTags(LaundryOrder order, int count) {
        OrderItem item = order.getItems().get(0);
        List<GarmentTag> tags = new ArrayList<>();
        for (int sequence = 1; sequence <= count; sequence++) {
            tags.add(new GarmentTag("TAG-EXISTING-" + sequence, order, item, sequence));
        }
        return tags;
    }
}