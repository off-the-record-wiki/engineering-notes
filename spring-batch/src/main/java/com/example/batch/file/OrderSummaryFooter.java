package com.example.batch.file;

import java.io.IOException;
import java.io.Writer;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.file.FlatFileFooterCallback;

public class OrderSummaryFooter implements FlatFileFooterCallback, ItemWriteListener<GradedOrder> {

    private long count;
    private long total;

    @Override
    public void afterWrite(Chunk<? extends GradedOrder> items) {
        for (GradedOrder order : items) {
            count++;
            total += order.amount();
        }
    }

    @Override
    public void writeFooter(Writer writer) throws IOException {
        writer.write("count=" + count + ",total=" + total);
    }
}
