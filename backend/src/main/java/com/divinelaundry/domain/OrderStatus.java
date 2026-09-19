package com.divinelaundry.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    DRAFT,
    RECEIVED,
    WASHING,
    IRONING,
    CLEANED,
    READY,
    DELIVERED,
    CANCELLED,
    REWORK;

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(RECEIVED, CANCELLED),
            RECEIVED, EnumSet.of(WASHING, IRONING, CLEANED, CANCELLED, REWORK),
            WASHING, EnumSet.of(IRONING, CLEANED, REWORK, CANCELLED),
            IRONING, EnumSet.of(CLEANED, READY, REWORK, CANCELLED),
            CLEANED, EnumSet.of(IRONING, READY, REWORK, CANCELLED),
            READY, EnumSet.of(DELIVERED, REWORK, CANCELLED),
            REWORK, EnumSet.of(WASHING, IRONING, CLEANED, READY, CANCELLED),
            DELIVERED, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class));

    public static boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null || from == to) {
            return false;
        }
        Set<OrderStatus> allowed = VALID_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }
}

