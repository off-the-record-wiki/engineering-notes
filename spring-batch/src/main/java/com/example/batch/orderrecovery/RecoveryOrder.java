package com.example.batch.orderrecovery;

import java.time.LocalDateTime;

public record RecoveryOrder(long id, long customerId, LocalDateTime orderDatetime, OrderStatus status, String shippingId) {

    public RecoveryOrder withStatus(OrderStatus newStatus) {
        return new RecoveryOrder(id, customerId, orderDatetime, newStatus, shippingId);
    }
}