package com.divinelaundry.service;

import com.divinelaundry.domain.Customer;
import com.divinelaundry.domain.GarmentTag;
import com.divinelaundry.domain.LaundryOrder;
import com.divinelaundry.repository.GarmentTagRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DocumentServiceTest {
    @Test
    void repeatedDocumentRequestsReuseTheSameTags() {
        OrderService orders = mock(OrderService.class);
        PaymentService payments = mock(PaymentService.class);
        TagService tags = mock(TagService.class);
        GarmentTagRepository garmentTags = mock(GarmentTagRepository.class);
        LaundryOrder order = new LaundryOrder("document-request", new Customer("Test Customer", "9876543210", null, "Trichy"), null, null, "admin");
        order.assignOrderNumber("SO-2026-000001");
        order.finalizeInvoice("INV-2026-000001");
        GarmentTag tag = new GarmentTag("TAG-2026-000001-01", order, null, 1);
        PaymentService.PaymentSummary summary = new PaymentService.PaymentSummary(order, java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO, List.of());
        when(orders.get(order.getOrderNumber())).thenReturn(order);
        when(tags.ensureTags(order)).thenReturn(List.of(tag));
        when(payments.summary(order.getOrderNumber())).thenReturn(summary);
        when(garmentTags.findByOrder_IdOrderByOrderItem_IdAscPieceSequenceAsc(order.getId())).thenReturn(List.of(tag));
        DocumentService service = new DocumentService(orders, payments, tags, garmentTags,
                "Divine Laundry", "0431-000000", "Trichy", "");

        DocumentService.DocumentBundle first = service.document(order.getOrderNumber());
        DocumentService.DocumentBundle second = service.document(order.getOrderNumber());

        assertThat(first.tags()).containsExactly(tag);
        assertThat(second.tags()).containsExactly(tag);
        verify(garmentTags, times(2)).findByOrder_IdOrderByOrderItem_IdAscPieceSequenceAsc(order.getId());
        verify(tags, never()).ensureTags(order);
    }
}