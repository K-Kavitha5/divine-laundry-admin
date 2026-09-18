package com.divinelaundry.web;

import jakarta.validation.constraints.*;

public class CustomerForm {
    @NotBlank @Size(max = 120)
    private String name = "";
    @NotBlank @Pattern(regexp = "[0-9+ ()-]{10,20}")
    private String phone = "";
    @Size(max = 255)
    private String addressLine = "";
    @Size(max = 120)
    private String area = "";
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { this.phone = value; }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String value) { this.addressLine = value; }
    public String getArea() { return area; }
    public void setArea(String value) { this.area = value; }
}

