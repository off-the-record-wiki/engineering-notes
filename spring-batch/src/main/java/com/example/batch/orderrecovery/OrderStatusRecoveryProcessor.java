package com.example.batch.orderrecovery;

import org.springframework.batch.infrastructure.item.ItemProcessor;

public class OrderStatusRecoveryProcessor implements ItemProcessor<RecoveryOrder, RecoveryOrder> {

    @Override
    public RecoveryOrder process(RecoveryOrder order) {
        OrderStatus recovered = hasShippingId(order) ? OrderStatus.SHIPPED : OrderStatus.READY_FOR_SHIPMENT;
        if (recovered == order.status()) {
            return null;
        }
        return order.withStatus(recovered);
    }

    private static boolean hasShippingId(RecoveryOrder order) {
        return order.shippingId() != null && !order.shippingId().isEmpty();
    }
}