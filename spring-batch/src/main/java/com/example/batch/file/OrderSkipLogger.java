package com.example.batch.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

public class OrderSkipLogger implements SkipListener<Order, GradedOrder> {

    private static final Logger log = LoggerFactory.getLogger(OrderSkipLogger.class);

    @Override
    public void onSkipInRead(Throwable throwable) {
        if (throwable instanceof FlatFileParseException parseException) {
            log.warn("skip line={} input={}", parseException.getLineNumber(), parseException.getInput());
            return;
        }
        log.warn("skip read error={}", throwable.toString());
    }
}
