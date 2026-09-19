package com.divinelaundry.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "alternate_phone", length = 20)
    private String alternatePhone;

    @Column(length = 160)
    private String email;

    @Column(name = "address_line")
    private String addressLine;

    @Column(length = 120)
    private String area;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Customer() {}

    public Customer(String name, String phone, String addressLine, String area) {
        this.name = name;
        this.phone = phone;
        this.addressLine = addressLine;
        this.area = area;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getAlternatePhone() { return alternatePhone; }
    public String getEmail() { return email; }
    public String getAddressLine() { return addressLine; }
    public String getArea() { return area; }
    public String getNotes() { return notes; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void update(String name, String phone, String alternatePhone, String email, String addressLine, String area, String notes, boolean active) {
        this.name = name;
        this.phone = phone;
        this.alternatePhone = alternatePhone;
        this.email = email;
        this.addressLine = addressLine;
        this.area = area;
        this.notes = notes;
        this.active = active;
    }
}

