package com.divinelaundry.api;

import com.divinelaundry.domain.GarmentTag;
import com.divinelaundry.domain.OrderItem;
import com.divinelaundry.domain.Payment;
import com.divinelaundry.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/documents/orders")
public class DocumentController {
    private final DocumentService documents;

    public DocumentController(DocumentService documents) {
        this.documents = documents;
    }

    @GetMapping("/{orderNumber}")
    DocumentResponse document(@PathVariable String orderNumber) {
        return DocumentResponse.from(documents.document(orderNumber));
    }

    @PostMapping("/{orderNumber}/tags/printed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markTagsPrinted(@PathVariable String orderNumber) {
        documents.markTagsPrinted(orderNumber);
    }

    public record BusinessResponse(String name, String phone, String address, String gstNumber) {}
    public record CustomerResponse(String name, String phone, String address) {}
    public record ItemResponse(
            String serviceCode,
            String serviceName,
            String unit,
            BigDecimal rate,
            BigDecimal quantity,
            int pieces,
            BigDecimal lineTotal) {
        static ItemResponse from(OrderItem item) {
            return new ItemResponse(
                    item.getServiceCode(), item.getServiceName(), item.getPricingUnit().name(),
                    item.getUnitRate(), item.getBillableQuantity(), item.getPieceCount(), item.getLineTotal());
        }
    }
    public record PaymentResponse(
            String paymentNumber,
            String mode,
            String transactionReference,
            BigDecimal amount,
            Instant paidAt) {
        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(
                    payment.getPaymentNumber(), payment.getMode().name(), payment.getTransactionReference(),
                    payment.getAmount(), payment.getPaidAt());
        }
    }
    public record TagResponse(
            String tagNumber,
            String serviceName,
            int pieceSequence,
            int printCount) {
        static TagResponse from(GarmentTag tag) {
            return new TagResponse(
                    tag.getTagNumber(), tag.getOrderItem().getServiceName(),
                    tag.getPieceSequence(), tag.getPrintCount());
        }
    }
    public record DocumentResponse(
            BusinessResponse business,
            String orderNumber,
            String invoiceNumber,
            Instant placedAt,
            Instant deliveryAt,
            CustomerResponse customer,
            String notes,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal tax,
            BigDecimal roundOff,
            BigDecimal total,
            BigDecimal amountPaid,
            BigDecimal balance,
            String paymentStatus,
            List<ItemResponse> items,
            List<PaymentResponse> payments,
            List<TagResponse> tags) {
        static DocumentResponse from(DocumentService.DocumentBundle bundle) {
            var order = bundle.order();
            var customer = order.getCustomer();
            String customerAddress = java.util.stream.Stream.of(customer.getAddressLine(), customer.getArea())
                    .filter(value -> value != null && !value.isBlank())
                    .collect(java.util.stream.Collectors.joining(", "));
            return new DocumentResponse(
                    new BusinessResponse(
                            bundle.business().name(), bundle.business().phone(),
                            bundle.business().address(), bundle.business().gstNumber()),
                    order.getOrderNumber(), order.getInvoiceNumber(), order.getPlacedAt(), order.getDeliveryAt(),
                    new CustomerResponse(customer.getName(), customer.getPhone(), customerAddress),
                    order.getNotes(), order.getSubtotal(), order.getDiscount(), order.getTax(),
                    order.getRoundOff(), order.getTotal(), bundle.paymentSummary().amountPaid(),
                    bundle.paymentSummary().balance(), order.getPaymentStatus().name(),
                    order.getItems().stream().map(ItemResponse::from).toList(),
                    bundle.paymentSummary().payments().stream().map(PaymentResponse::from).toList(),
                    bundle.tags().stream().map(TagResponse::from).toList());
        }
    }
}
