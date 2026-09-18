package com.divinelaundry.service;

public record PaymentRecordedEvent(String orderNumber, String paymentNumber) {}
