package com.divinelaundry.web;

import com.divinelaundry.domain.*;
import com.divinelaundry.repository.*;
import com.divinelaundry.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service
public class WebOrderService {
    private final CustomerRepository customers;
    private final LaundryServiceRepository catalog;
    private final OrderService orders;
    private final ZoneId zone;
    public WebOrderService(CustomerRepository customers, LaundryServiceRepository catalog,
            OrderService orders, @Value("${app.business-zone}") String zone) {
        this.customers = customers; this.catalog = catalog; this.orders = orders; this.zone = ZoneId.of(zone);
    }

    @Transactional
    public String create(OrderForm form, String username) {
        // Serialize submissions for one customer across tabs/app instances. The unique
        // request ID still allows the same customer to have multiple legitimate orders.
        customers.lockForOrder(form.getCustomerId()).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        Instant delivery;
        try { delivery = LocalDateTime.parse(form.getDeliveryAt()).atZone(zone).toInstant(); }
        catch (java.time.DateTimeException ex) { throw new IllegalArgumentException("Enter a valid delivery date and time"); }
        var lines = form.getItems().stream().map(line -> {
            var item = catalog.findById(line.getServiceId()).filter(LaundryServiceItem::isActive)
                    .orElseThrow(() -> new IllegalArgumentException("Select an active service"));
            if (item.getUnitRate().signum() <= 0) throw new IllegalArgumentException("Price is not confirmed for " + item.getName());
            if (item.getPricingUnit() == PricingUnit.PIECE &&
                    (line.getQuantity().stripTrailingZeros().scale() > 0 || line.getQuantity().intValueExact() != line.getPieces())) {
                throw new IllegalArgumentException("For per-piece services, quantity and physical piece count must match");
            }
            return new OrderService.CreateOrderItem(line.getServiceId(), line.getQuantity(), line.getPieces(), false);
        }).toList();
        if (form.getItems().stream().mapToInt(LineForm::getPieces).sum() > 500) {
            throw new IllegalArgumentException("Maximum 500 physical pieces per order in this milestone");
        }
        var order = orders.create(new OrderService.CreateOrderCommand(form.getRequestId(), form.getCustomerId(),
                delivery, form.getNotes(), username, form.getDiscount(), form.getTax(), lines));
        orders.finalizeInvoice(order.getOrderNumber());
        return order.getOrderNumber();
    }
}
