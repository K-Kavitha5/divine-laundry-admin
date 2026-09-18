package com.divinelaundry.web;

import com.divinelaundry.domain.Customer;
import com.divinelaundry.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerWebService {
    private final CustomerRepository customers;
    public CustomerWebService(CustomerRepository customers) { this.customers = customers; }
    @Transactional
    public Customer create(CustomerForm form) {
        String phone = form.getPhone().replaceAll("[^0-9]", "");
        if (phone.length() == 12 && phone.startsWith("91")) phone = phone.substring(2);
        if (!phone.matches("[6-9][0-9]{9}")) throw new IllegalArgumentException("Enter a valid 10-digit Indian mobile number");
        if (customers.existsByPhone(phone)) throw new IllegalArgumentException("This mobile number already has a customer. Select the existing customer.");
        return customers.saveAndFlush(new Customer(form.getName().trim(), phone, form.getAddressLine().trim(), form.getArea().trim()));
    }
}
