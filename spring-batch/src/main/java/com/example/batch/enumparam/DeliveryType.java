package com.example.batch.enumparam;

public enum DeliveryType {
    STANDARD(3),
    EXPRESS(1);

    private final int leadDays;

    DeliveryType(int leadDays) {
        this.leadDays = leadDays;
    }

    public int leadDays() {
        return leadDays;
    }
}