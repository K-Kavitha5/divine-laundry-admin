package com.divinelaundry.web;

import jakarta.validation.constraints.*;

public class LineForm {
    @NotNull
    private Long serviceId = null;
    @NotNull @DecimalMin("0.001") @DecimalMax("9999") @Digits(integer = 4, fraction = 3)
    private java.math.BigDecimal quantity = java.math.BigDecimal.ONE;
    @NotNull @Min(1) @Max(500)
    private Integer pieces = 1;
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long value) { this.serviceId = value; }
    public java.math.BigDecimal getQuantity() { return quantity; }
    public void setQuantity(java.math.BigDecimal value) { this.quantity = value; }
    public Integer getPieces() { return pieces; }
    public void setPieces(Integer value) { this.pieces = value; }
}

