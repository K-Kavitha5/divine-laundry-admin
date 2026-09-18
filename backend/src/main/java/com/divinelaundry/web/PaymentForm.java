package com.divinelaundry.web;

import jakarta.validation.constraints.*;

public class PaymentForm {
    @NotBlank @Pattern(regexp = "[a-f0-9-]{36}")
    private String requestId = java.util.UUID.randomUUID().toString();
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2)
    private java.math.BigDecimal amount = null;
    @NotNull
    private com.divinelaundry.domain.PaymentMode mode = com.divinelaundry.domain.PaymentMode.CASH;
    @Size(max = 120)
    private String reference = "";
    public String getRequestId() { return requestId; }
    public void setRequestId(String value) { this.requestId = value; }
    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal value) { this.amount = value; }
    public com.divinelaundry.domain.PaymentMode getMode() { return mode; }
    public void setMode(com.divinelaundry.domain.PaymentMode value) { this.mode = value; }
    public String getReference() { return reference; }
    public void setReference(String value) { this.reference = value; }
}

