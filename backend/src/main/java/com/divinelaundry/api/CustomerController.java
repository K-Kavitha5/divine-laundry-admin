package com.divinelaundry.api;

import com.divinelaundry.domain.Customer;
import com.divinelaundry.repository.CustomerRepository;
import com.divinelaundry.web.CustomerWebService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerRepository customers;
    private final CustomerWebService customerService;

    public CustomerController(CustomerRepository customers, CustomerWebService customerService) {
        this.customers = customers;
        this.customerService = customerService;
    }

    @GetMapping
    List<CustomerResponse> search(@RequestParam(defaultValue = "") String q) {
        String phoneQuery = q;
        try {
            phoneQuery = customerService.normalizePhone(q);
        } catch (IllegalArgumentException ignored) {
            // Name searches should retain the original query for both repository predicates.
        }
        return customers.findTop30ByNameContainingIgnoreCaseOrPhoneContainingOrderByNameAsc(q, phoneQuery)
                .stream().map(CustomerResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
        String phone = customerService.normalizePhone(request.phone());
        if (customers.existsByPhone(phone)) {
            throw new IllegalArgumentException("A customer with this phone number already exists");
        }
        Customer customer = new Customer(request.name().trim(), phone, request.addressLine(), request.area());
        customer.update(request.name(), request.alternatePhone(), request.email(), request.addressLine(), request.area(), request.notes());
        return CustomerResponse.from(customers.save(customer));
    }

    public record CustomerRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "^[0-9+ -]{8,20}$") String phone,
            String alternatePhone,
            String email,
            String addressLine,
            String area,
            String notes) {}

    public record CustomerResponse(Long id, String name, String phone, String addressLine, String area) {
        static CustomerResponse from(Customer c) {
            return new CustomerResponse(c.getId(), c.getName(), c.getPhone(), c.getAddressLine(), c.getArea());
        }
    }
}

