package com.example.batch.file;

import org.springframework.batch.infrastructure.item.ItemProcessor;

public class OrderGradeProcessor implements ItemProcessor<Order, GradedOrder> {

    private static final long VIP_THRESHOLD = 10_000L;

    @Override
    public GradedOrder process(Order order) {
        String grade = order.amount() >= VIP_THRESHOLD ? "VIP" : "NORMAL";
        return new GradedOrder(order.orderId(), order.customer(), order.amount(), grade);
    }
}
