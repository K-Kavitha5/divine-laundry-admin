package com.divinelaundry.web;

import jakarta.validation.constraints.*;

public class OrderForm {
    @NotBlank @Pattern(regexp = "[a-f0-9-]{36}")
    private String requestId = java.util.UUID.randomUUID().toString();
    @NotNull
    private Long customerId = null;
    @NotBlank @Size(max = 30)
    private String deliveryAt = "";
    @Size(max = 800)
    private String notes = "";
    @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2)
    private java.math.BigDecimal discount = java.math.BigDecimal.ZERO;
    @NotNull @DecimalMin("0") @Digits(integer = 8, fraction = 2)
    private java.math.BigDecimal tax = java.math.BigDecimal.ZERO;
    @jakarta.validation.Valid @Size(min = 1, max = 50)
    private java.util.List<@NotNull LineForm> items = new java.util.ArrayList<>();
    public String getRequestId() { return requestId; }
    public void setRequestId(String value) { this.requestId = value; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long value) { this.customerId = value; }
    public String getDeliveryAt() { return deliveryAt; }
    public void setDeliveryAt(String value) { this.deliveryAt = value; }
    public String getNotes() { return notes; }
    public void setNotes(String value) { this.notes = value; }
    public java.math.BigDecimal getDiscount() { return discount; }
    public void setDiscount(java.math.BigDecimal value) { this.discount = value; }
    public java.math.BigDecimal getTax() { return tax; }
    public void setTax(java.math.BigDecimal value) { this.tax = value; }
    public java.util.List<LineForm> getItems() { return items; }
    public void setItems(java.util.List<LineForm> value) { this.items = value; }
}
