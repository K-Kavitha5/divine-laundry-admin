package com.divinelaundry.web;

import com.divinelaundry.domain.Customer;
import com.divinelaundry.domain.LaundryOrder;
import com.divinelaundry.domain.OrderStatus;
import com.divinelaundry.repository.CustomerRepository;
import com.divinelaundry.repository.LaundryOrderRepository;
import com.divinelaundry.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerWebService {
    private final CustomerRepository customers;
    private final LaundryOrderRepository orders;
    private final PaymentRepository payments;
    public CustomerWebService(CustomerRepository customers, LaundryOrderRepository orders, PaymentRepository payments) {
        this.customers = customers;
        this.orders = orders;
        this.payments = payments;
    }
    @Transactional
    public Customer create(CustomerForm form) {
        String phone = normalizePhone(form.getPhone());
        if (customers.existsByPhone(phone)) throw new IllegalArgumentException("This mobile number already has a customer. Select the existing customer.");
        return customers.saveAndFlush(new Customer(form.getName().trim(), phone, form.getAddressLine().trim(), form.getArea().trim()));
    }

    public String normalizePhone(String value) {
        String phone = value == null ? "" : value.replaceAll("[^0-9]", "");
        if (phone.length() == 12 && phone.startsWith("91")) phone = phone.substring(2);
        if (!phone.matches("[6-9][0-9]{9}")) throw new IllegalArgumentException("Enter a valid 10-digit Indian mobile number");
        return phone;
    }

    @Transactional(readOnly = true)
    public CustomerProfile profile(Long id) {
        Customer customer = customers.findById(id).orElseThrow(() -> new CustomerNotFoundException());
        List<CustomerOrder> history = orders.findByCustomerIdOrderByPlacedAtDesc(id).stream()
                .map(this::toOrder)
                .toList();
        BigDecimal billed = history.stream().filter(CustomerOrder::billable).map(CustomerOrder::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = history.stream().map(CustomerOrder::paid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CustomerProfile(customer, history.size(), billed, paid, billed.subtract(paid).max(BigDecimal.ZERO), history);
    }

    @Transactional(readOnly = true)
    public List<CustomerListRow> listRows(List<Customer> rows) {
        return rows.stream().map(customer -> {
            CustomerProfile profile = profile(customer.getId());
            return new CustomerListRow(customer, profile.orderCount(), profile.outstanding());
        }).toList();
    }

    @Transactional
    public Customer update(Long id, CustomerForm form) {
        Customer customer = customers.findById(id).orElseThrow(() -> new CustomerNotFoundException());
        String phone = normalizePhone(form.getPhone());
        customers.findByPhone(phone).filter(existing -> !Objects.equals(existing.getId(), id))
                .ifPresent(existing -> { throw new IllegalArgumentException("This mobile number already has a customer."); });
        customer.update(form.getName().trim(), phone, blankToNull(form.getAlternatePhone()), blankToNull(form.getEmail()),
                blankToNull(form.getAddressLine()), blankToNull(form.getArea()), blankToNull(form.getNotes()), form.isActive());
        return customers.saveAndFlush(customer);
    }

    private CustomerOrder toOrder(LaundryOrder order) {
        BigDecimal paid = payments.sumByOrderId(order.getId());
        return new CustomerOrder(order, paid, order.getTotal().subtract(paid).max(BigDecimal.ZERO),
                order.getInvoiceNumber() != null && order.getWorkStatus() != OrderStatus.CANCELLED
                        && order.getWorkStatus() != OrderStatus.DRAFT);
    }

    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record CustomerListRow(Customer customer, int orderCount, BigDecimal outstanding) {}
    public record CustomerProfile(Customer customer, int orderCount, BigDecimal billed, BigDecimal paid,
            BigDecimal outstanding, List<CustomerOrder> orders) {}
    public record CustomerOrder(LaundryOrder order, BigDecimal paid, BigDecimal balance, boolean billable) {
        public BigDecimal total() { return order.getTotal(); }
    }
    public static class CustomerNotFoundException extends RuntimeException {}
}
