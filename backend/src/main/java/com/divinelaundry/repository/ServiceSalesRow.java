package com.divinelaundry.repository;

import java.math.BigDecimal;

public record ServiceSalesRow(
        String serviceCode,
        String serviceName,
        BigDecimal sales,
        Long orderCount,
        BigDecimal billableQuantity,
        Long physicalPieces) {}
