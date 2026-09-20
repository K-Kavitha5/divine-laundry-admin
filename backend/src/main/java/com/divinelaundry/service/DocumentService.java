package com.divinelaundry.service;

import com.divinelaundry.domain.GarmentTag;
import com.divinelaundry.domain.LaundryOrder;
import com.divinelaundry.repository.GarmentTagRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class DocumentService {
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final TagService tagService;
    private final GarmentTagRepository garmentTags;
    private final BusinessDetails business;

    public DocumentService(
            OrderService orderService,
            PaymentService paymentService,
            TagService tagService,
            GarmentTagRepository garmentTags,
            @Value("${app.business.name}") String name,
            @Value("${app.business.phone}") String phone,
            @Value("${app.business.address}") String address,
            @Value("${app.business.gst-number:}") String gstNumber) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.tagService = tagService;
        this.garmentTags = garmentTags;
        this.business = new BusinessDetails(name, phone, address, gstNumber);
    }

    @Transactional(readOnly = true)
    public DocumentBundle document(String orderNumber) {
        LaundryOrder order = orderService.get(orderNumber);
        if (order.getInvoiceNumber() == null) {
            throw new IllegalStateException("Create the invoice before printing or sharing it");
        }
        List<GarmentTag> tags = garmentTags.findByOrder_IdOrderByOrderItem_IdAscPieceSequenceAsc(order.getId());
        PaymentService.PaymentSummary payments = paymentService.summary(orderNumber);
        return new DocumentBundle(business, order, payments, tags);
    }

    @Transactional
    public void markTagsPrinted(String orderNumber) {
        LaundryOrder order = orderService.get(orderNumber);
        tagService.markPrinted(order);
    }

    public record BusinessDetails(String name, String phone, String address, String gstNumber) {}
    public record DocumentBundle(
            BusinessDetails business,
            LaundryOrder order,
            PaymentService.PaymentSummary paymentSummary,
            List<GarmentTag> tags) {}
}
